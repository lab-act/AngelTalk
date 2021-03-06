package angeltalk.plus.presentation.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.RequestManager;

import java.io.File;
import java.util.List;

import angeltalk.plus.R;
import angeltalk.plus.domain.model.CardModel;
import angeltalk.plus.presentation.custom.AddCardView;
import angeltalk.plus.presentation.custom.CardView;
import angeltalk.plus.presentation.custom.VideoCardTextureView;
import angeltalk.plus.presentation.util.ContentsUtil;
import angeltalk.plus.presentation.util.PlayUtil;

public class CardImageAdapter extends PagerAdapter {

    private Context context;
    private final RequestManager glide;
    private boolean hasNewCardView;
    private List<CardModel> dataList;
    public SparseArray<View> viewCollection = new SparseArray<>();
    private PlayUtil playUtil;
    private boolean isNotLongClicked;
    private CardView lastSelectedCardView = null;
    private Handler speakHandler = new Handler();

    public CardImageAdapter(Context context, List<CardModel> dataList, RequestManager glide) {
        this.context = context;
        this.dataList = dataList;
        this.glide = glide;

        hasNewCardView = false;

        playUtil = PlayUtil.getInstance();
        playUtil.initTts(context.getApplicationContext());
    }

    public View getItemAt(int index) {
        return viewCollection.get(index);
    }

    public void removeItemAt(int index) { dataList.remove(index); }

    @Override
    public int getCount() {
        if (dataList == null) {
            return 0;
        } else {
            return dataList.size();
        }
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof AddCardView) {
            return 0;
        }
        CardView cardView = (CardView) object;
        int position = dataList.indexOf(cardView.dataModel);
        return position == -1 ? POSITION_NONE : position;
    }

    @Override
    public View instantiateItem(final ViewGroup container, int position) {
        if (hasNewCardView && position == 0) {
            AddCardView cardView = new AddCardView(context);
            container.addView(cardView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            viewCollection.put(position, cardView);
            return cardView;
        } else {
            final CardView cardView = new CardView(context);
            final CardModel singleSectionItems = dataList.get(position);

            if (singleSectionItems.cardType == CardModel.CardType.PHOTO_CARD) {
                cardView.findViewById(R.id.card_image).setVisibility(View.VISIBLE);
                cardView.findViewById(R.id.card_video).setVisibility(View.GONE);
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inPreferredConfig = Bitmap.Config.ARGB_8888;
                cardView.cardImage.setImageBitmap(BitmapFactory.decodeFile( singleSectionItems.contentPath));
            } else if (singleSectionItems.cardType == CardModel.CardType.VIDEO_CARD) {
                BitmapFactory.Options option = new BitmapFactory.Options();
                option.inPreferredConfig = Bitmap.Config.ARGB_8888;
                cardView.cardImage.setImageBitmap(BitmapFactory.decodeFile( singleSectionItems.thumbnailPath));

                VideoCardTextureView cardVideoView = ((VideoCardTextureView) cardView.findViewById(R.id.card_video));
                cardVideoView.setVisibility(View.VISIBLE);
                cardVideoView.setScaleType(VideoCardTextureView.ScaleType.CENTER_CROP);
                File video = ContentsUtil.getContentFile(singleSectionItems.contentPath);
                if (video != null) {
                    cardVideoView.setDataSource(video.getAbsolutePath());
                }
            }

            cardView.setDataModel(dataList.get(position));
            cardView.cardImage.setScaleType(ImageView.ScaleType.FIT_XY);

            cardView.cardTitle.setText(singleSectionItems.name);

            View cardContainer = cardView.findViewById(R.id.card_container);
            cardContainer.setOnClickListener(cardContainerOnClickListener);
            cardContainer.setOnLongClickListener(cardContainerOnLongClickListener);

            isNotLongClicked = true;

            container.addView(cardView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            viewCollection.put(position, cardView);
            return cardView;
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        viewCollection.remove(position);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void addNewCardViewAtFirst() {
        hasNewCardView = true;
        CardModel emptyModel = CardModel.builder().cardIndex(-1).build();
        dataList.add(0, emptyModel);
    }

    private View.OnClickListener cardContainerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getParent().getParent() instanceof CardView) {
                if (isNotLongClicked) {
                    CardView cardView = (CardView) v.getParent().getParent();
                    startCardSelectionEffect(cardView);
                } else {
                    isNotLongClicked = true;
                }
            }
        }
    };

    private View.OnLongClickListener cardContainerOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (v.getParent().getParent() instanceof CardView) {
                isNotLongClicked = false;
                CardView cardView = (CardView) v.getParent().getParent();
                startCardSelectionEffect(cardView);
                return false;
            } else {
                return true;
            }
        }
    };


    private void startCardSelectionEffect(CardView cardView) {
        lastSelectedCardView = cardView;
        if (cardView.mode == CardView.MODE_VIEW_CARD) {
            cardView.bringToFront();

            if (cardView.status == CardView.CardViewStatus.CARD_TITLE_SHOWN) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(500);
            }

            AnimationSet animSet = new AnimationSet(true);
            Animation zoomOutAnimation = AnimationUtils.loadAnimation(context, R.anim.zoom_out);
            animSet.addAnimation(zoomOutAnimation);
            cardView.startAnimation(animSet);

            if (cardView.dataModel.cardType == CardModel.CardType.VIDEO_CARD) {
                final VideoCardTextureView cardVideoView = ((VideoCardTextureView) cardView.findViewById(R.id.card_video));
                if (cardVideoView != null) {
                    cardVideoView.play(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            cardVideoView.resetPlayer();
                        }
                    });

                }
                startSpeakCardName(cardView, 3500); //Video card
            } else {
                startSpeakCardName(cardView, 500); //Text card
            }
        }
    }

    private void startSpeakCardName(final CardView cardView, int delayMillis) {
        //Handler handler = new Handler();
        speakHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (voiceFileExists(cardView)) {
                    playUtil.play(cardView.dataModel.voicePath);
                } else {
                    playUtil.ttsSpeak(cardView.cardTitle.getText().toString());
                }
            }
        }, delayMillis);
    }

    public void stopVideoView() {
        if (lastSelectedCardView != null) {
            VideoCardTextureView cardVideoView = ((VideoCardTextureView) lastSelectedCardView.findViewById(R.id.card_video));
            if (cardVideoView != null && cardVideoView.isPlaying()) {
                cardVideoView.stop();
                cardVideoView.seekTo(0);
            }
        }
    }

    public void releaseSpeakHandler() {
        playUtil.playStop();
        playUtil.ttsStop();
        speakHandler.removeCallbacksAndMessages(null);
    }

    private boolean voiceFileExists(CardView cardView) {
        return cardView.dataModel != null &&
                cardView.dataModel.voicePath != null &&
                (new File(cardView.dataModel.voicePath)).exists();
    }
}
