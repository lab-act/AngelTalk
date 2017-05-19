package act.angelman.presentation.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import act.angelman.AngelmanApplication;
import act.angelman.R;
import act.angelman.domain.model.CardModel;
import act.angelman.domain.model.CardTransferModel;
import act.angelman.domain.model.CategoryModel;
import act.angelman.domain.repository.CardRepository;
import act.angelman.domain.repository.CategoryRepository;
import act.angelman.network.transfer.CardTransfer;
import act.angelman.presentation.adapter.CardImageAdapter;
import act.angelman.presentation.custom.CardTitleLayout;
import act.angelman.presentation.custom.CardViewPager;
import act.angelman.presentation.custom.CategorySelectDialog;
import act.angelman.presentation.listener.OnDownloadCompleteListener;
import act.angelman.presentation.manager.ApplicationConstants;
import act.angelman.presentation.manager.ApplicationManager;
import act.angelman.presentation.util.ContentsUtil;
import act.angelman.presentation.util.FileUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ShareCardActivity extends AbstractActivity {

    @Inject
    CardTransfer cardTransfer;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    CardRepository cardRepository;

    @Inject
    ApplicationManager applicationManager;

    @BindView(R.id.title_container)
    CardTitleLayout titleLayout;

    @BindView(R.id.view_pager)
    CardViewPager mViewPager;

    @BindView(R.id.category_item_title)
    TextView categoryTitle;

    @BindView(R.id.image_angelee_gif)
    ImageView imageLoadingGif;

    @BindView(R.id.on_loading_view)
    LinearLayout loadingViewLayout;

    private CategorySelectDialog categorySelectDialog;

    private RequestManager glide;
    private String receiveKey;
    private Context context;

    private CardTransferModel shareCardModel;
    private String shareFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_card);
        ButterKnife.bind(this);
        ((AngelmanApplication) getApplication()).getAngelmanComponent().inject(this);

        this.glide = Glide.with(this);
        this.context = getApplicationContext();

        initView();

        if (getString(R.string.kakao_scheme).equals(getIntent().getScheme())) {
            Uri uri = getIntent().getData();
            receiveKey = uri.getQueryParameter("key");
        }else if ("http".equals(getIntent().getScheme())) {
            Uri uri = getIntent().getData();
            receiveKey = uri.getQueryParameter("key");
        }
        downloadCard();
    }

    @Override
    public void onBackPressed() {
        moveToCategoryMenuActivity();
    }

    private void initView() {
        showLoadingAnimation();
        titleLayout.setCategoryModelTitle(getApplicationContext().getString(R.string.new_card_title));
        titleLayout.hideCardCountText();
        titleLayout.hideListCardButton();
        titleLayout.setBackButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveToCategoryMenuActivity();
            }
        });
    }

    private void showLoadingAnimation() {
        Glide.with(ShareCardActivity.this)
                .load(R.drawable.angelee)
                .asGif()
                .crossFade()
                .into(imageLoadingGif);
    }


    public void downloadCard() {
        cardTransfer.downloadCard(receiveKey, new OnDownloadCompleteListener() {
            @Override
            public void onSuccess(CardTransferModel cardTransferModel, String filePath) {
                try {
                    shareCardModel = cardTransferModel;
                    shareFilePath = filePath;

                    String tempLocation = context.getCacheDir() + File.separator + receiveKey;
                    FileUtil.unzip(filePath, tempLocation);

                    List<CardModel> cardModelList = Lists.newArrayList();
                    CardModel cardModel = ContentsUtil.getTempCardModel(tempLocation, cardTransferModel);

                    cardModelList.add(cardModel);

                    mViewPager.setAdapter(new CardImageAdapter(context, cardModelList, glide));

                    loadingViewLayout.setVisibility(View.GONE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail() {
                FileUtil.removeFilesIn(ContentsUtil.getTempFolder(context));
            }
        });
    }

    @OnClick(R.id.card_save_button)
    public void onClickCardSaveButton(View view) {
        categorySelectDialog = new CategorySelectDialog(ShareCardActivity.this, categoryRepository.getCategoryAllList(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareCardModel != null) {
                    try {
                        CategoryModel selectItem = categorySelectDialog.getSelectItem();
                        if(selectItem == null) {
                            return;
                        }
                        FileUtil.unzip(shareFilePath, ContentsUtil.getTempFolder(context));
                        CardModel cardModel = saveNewSharedCard(shareCardModel, selectItem.index);
                        ContentsUtil.copySharedFiles(context, cardModel);
                        FileUtil.removeFilesIn(ContentsUtil.getTempFolder(context));

                        applicationManager.setCategoryModel(selectItem);
                        applicationManager.setCurrentCardIndex(cardModel.cardIndex);
                        moveToCardListActivity();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(context, R.string.cannot_share_card_save_message,Toast.LENGTH_SHORT).show();
                }
            }
        });
        categorySelectDialog.show();
    }

    private void moveToCategoryMenuActivity() {
        Intent intent = new Intent(context, CategoryMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void moveToCardListActivity() {
        Intent intent = new Intent(getApplicationContext(), CardListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ApplicationConstants.INTENT_KEY_SHARE_CARD, true);
        startActivity(intent);
        finish();
    }

    private CardModel saveNewSharedCard(CardTransferModel cardTransferModel, int categoryIndex) {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        CardModel.CardType cardType = CardModel.CardType.valueOf(cardTransferModel.cardType);

        String contentPath = cardType == CardModel.CardType.VIDEO_CARD ? ContentsUtil.getVideoPath(context) : ContentsUtil.getImagePath(context);

        CardModel cardModel = CardModel.builder()
                .name(cardTransferModel.name)
                .contentPath(contentPath)
                .voicePath(ContentsUtil.getVoicePath(context))
                .firstTime(dateFormat.format(date))
                .categoryId(categoryIndex)
                .cardType(cardType).thumbnailPath(cardType == CardModel.CardType.VIDEO_CARD ? ContentsUtil.getThumbnailPath(contentPath) : null)
                .hide(false)
                .build();

        cardRepository.createSingleCardModel(cardModel);

        return cardModel;
    }

    @VisibleForTesting String getReceiveKey(){
        return receiveKey;
    }


}
