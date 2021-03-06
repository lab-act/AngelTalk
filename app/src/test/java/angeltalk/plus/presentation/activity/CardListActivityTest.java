package angeltalk.plus.presentation.activity;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import angeltalk.plus.R;
import angeltalk.plus.TestAngelmanApplication;
import angeltalk.plus.UITest;
import angeltalk.plus.domain.model.CardModel;
import angeltalk.plus.domain.model.CategoryModel;
import angeltalk.plus.domain.repository.CardRepository;
import angeltalk.plus.presentation.manager.ApplicationConstants;
import angeltalk.plus.presentation.manager.ApplicationManager;
import angeltalk.plus.presentation.shadow.ShadowSnackbar;
import angeltalk.plus.presentation.util.ContentsUtil;
import angeltalk.plus.presentation.util.ResourcesUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowSnackbar.class})
public class CardListActivityTest extends UITest{

    @Inject
    ApplicationManager applicationManager;

    @Inject
    CardRepository cardRepository;

    private CardListActivity subject;

    @Before
    public void setUp() throws Exception {
        ((TestAngelmanApplication) RuntimeEnvironment.application).getAngelmanTestComponent().inject(this);
        when(applicationManager.getCategoryModel()).thenReturn(getCategoryModel());
        when(cardRepository.getSingleCardListWithCategoryId(applicationManager.getCategoryModel().index)).thenReturn(getCardModelList());
        subject = setupActivity(CardListActivity.class);
    }

    @Test
    public void whenLaunched_thenShowTitleCorrectly() throws Exception {
        assertThat(shadowOf(subject.titleLayout.getBackground()).getCreatedFromResId()).isEqualTo(R.color.simple_background_red);
        assertThat(subject.categoryItemTitle.getText()).isEqualTo("먹을 것");
    }

    @Test
    public void whenLaunched_thenShowCardListInCategory() throws Exception {
        assertThat(subject.showHideRecyclerView).isNotNull();
        assertThat(subject.showHideRecyclerView.getAdapter().getItemCount()).isEqualTo(8);
    }

    @Test
    public void givenShowingCardInList_whenLaunched_thenShowIcon() throws Exception {
        ImageView showHideBarView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.show_hide_item_bar));
        ImageView showHideIconView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.show_hide_icon));
        TextView cardName = ((TextView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.card_name));
        ImageView cardThumbnail = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.card_thumbnail));

        assertThat(shadowOf(showHideBarView.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.show_red);
        assertThat(shadowOf(showHideIconView.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.ic_show_red);
        assertThat(cardName.getCurrentTextColor()).isEqualTo(subject.getResources().getColor(R.color.black_00));
        assertThat(cardThumbnail.getImageAlpha()).isEqualTo(255);
    }

    @Test
    public void givenHidingCardInList_whenLaunched_thenHideIcon() throws Exception {
        View secondItemView = subject.showHideRecyclerView.getChildAt(2);

        ImageView showHideBarView = ((ImageView) secondItemView.findViewById(R.id.show_hide_item_bar));
        ImageView hideBarView = ((ImageView) secondItemView.findViewById(R.id.hide_item_bar));
        ImageView showHideIconView = ((ImageView) secondItemView.findViewById(R.id.show_hide_icon));
        TextView cardName = ((TextView) secondItemView.findViewById(R.id.card_name));
        ImageView cardThumbnail = ((ImageView) secondItemView.findViewById(R.id.card_thumbnail));

        assertThat(showHideBarView.getVisibility()).isEqualTo(GONE);
        assertThat(hideBarView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(showHideIconView.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.ic_show_red);
        assertThat(cardName.getCurrentTextColor()).isEqualTo(subject.getResources().getColor(R.color.black_4C));
        assertThat(cardThumbnail.getImageAlpha()).isEqualTo(60);
    }

    @Test
    public void whenClickShowHideRecyclerViewItem_thenShowHideChangeAndDatabaseUpdate() throws Exception {

        ImageView showHideBarView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.show_hide_item_bar));
        ImageView hideBarView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.hide_item_bar));
        assertThat(shadowOf(showHideBarView.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.show_red);

        // when
        subject.showHideRecyclerView.getChildAt(0).performClick();

        // then
        showHideBarView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.show_hide_item_bar));
        ImageView showHideIconView = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.show_hide_icon));
        TextView cardName = ((TextView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.card_name));
        ImageView cardThumbnail = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.card_thumbnail));

        assertThat(showHideBarView.getVisibility()).isEqualTo(GONE);
        assertThat(hideBarView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(showHideIconView.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.ic_show_red);
        assertThat(cardName.getCurrentTextColor()).isEqualTo(subject.getResources().getColor(R.color.black_4C));
        assertThat(cardThumbnail.getImageAlpha()).isEqualTo(60);

        verify(cardRepository).updateSingleCardModelHide(any(CardModel.class));
    }


    @Test
    public void givenShowChangeOrderRecyclerView_whenItemOrderChanged_thenDatabaseUpdate() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();

        // when
        RecyclerView.ViewHolder source = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(0));
        RecyclerView.ViewHolder target = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(1));

        subject.itemTouchHelperCallback.onSelectedChanged(source, ItemTouchHelper.ACTION_STATE_DRAG);
        subject.itemTouchHelperCallback.onMove(subject.changeOrderRecyclerView, source, target);
        subject.itemTouchHelperCallback.onSelectedChanged(target, ItemTouchHelper.ACTION_STATE_IDLE);

        verify(cardRepository).updateCategoryCardIndex(any(List.class));
    }

    @Test
    public void givenShowChangeOrderRecyclerView_whenItemOrderChanged_thenViewChanged() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();

        // when
        RecyclerView.ViewHolder source = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(0));
        RecyclerView.ViewHolder target = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(1));

        assertThat(((TextView) source.itemView.findViewById(R.id.card_name)).getText()).isEqualTo("물0");
        assertThat(((TextView) target.itemView.findViewById(R.id.card_name)).getText()).isEqualTo("물1");

        subject.itemTouchHelperCallback.onSelectedChanged(source, ItemTouchHelper.ACTION_STATE_DRAG);
        subject.itemTouchHelperCallback.onMove(subject.changeOrderRecyclerView, source, target);
        subject.itemTouchHelperCallback.onSelectedChanged(target, ItemTouchHelper.ACTION_STATE_IDLE);

        // then
        RecyclerView.ViewHolder first = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(0));
        RecyclerView.ViewHolder second = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(1));
        assertThat(((TextView) first.itemView.findViewById(R.id.card_name)).getText()).isEqualTo("물1");
        assertThat(((TextView) second.itemView.findViewById(R.id.card_name)).getText()).isEqualTo("물0");
    }

    @Test
    public void givenShowChangeOrderRecyclerView_whenItemOrderChanged_thenChangeCurrentIndex() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();

        // when
        RecyclerView.ViewHolder source = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(0));
        RecyclerView.ViewHolder target = subject.changeOrderRecyclerView.getChildViewHolder(subject.changeOrderRecyclerView.getChildAt(1));

        when(applicationManager.getCurrentCardIndex()).thenReturn(1);

        subject.itemTouchHelperCallback.onSelectedChanged(source, ItemTouchHelper.ACTION_STATE_DRAG);
        subject.itemTouchHelperCallback.onMove(subject.changeOrderRecyclerView, source, target);
        subject.itemTouchHelperCallback.onSelectedChanged(target, ItemTouchHelper.ACTION_STATE_IDLE);

        // then
        verify(applicationManager).setCurrentCardIndex(8);
    }

    @Test
    public void givenChangeHideItemStatus_whenClickedChangeOrderTabButton_thenItemShowHideStatus() throws Exception {
        // given
        subject.showHideRecyclerView.getChildAt(0).performClick();

        // when
        subject.changeOrderTabButton.performClick();

        // then
        View firstItem = subject.changeOrderRecyclerView.getChildAt(0);
        ImageView showHideBarView = ((ImageView) firstItem.findViewById(R.id.show_hide_item_bar));
        ImageView hideBarView = ((ImageView) firstItem.findViewById(R.id.hide_item_bar));
        ImageView itemMoveIcon = ((ImageView) firstItem.findViewById(R.id.item_move_icon));
        TextView cardName = ((TextView) firstItem.findViewById(R.id.card_name));
        ImageView cardThumbnail = ((ImageView) firstItem.findViewById(R.id.card_thumbnail));

        assertThat(showHideBarView.getVisibility()).isEqualTo(GONE);
        assertThat(hideBarView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(itemMoveIcon.getDrawable()).getCreatedFromResId()).isEqualTo(R.drawable.ic_list_red);
        assertThat(cardName.getCurrentTextColor()).isEqualTo(subject.getResources().getColor(R.color.black_4C));
        assertThat(cardThumbnail.getImageAlpha()).isEqualTo(60);
    }

    @Test
    public void whenClickBackButton_thenFinishActivity() throws Exception {
        subject.findViewById(R.id.back_button).performClick();
        assertThat(subject.isFinishing()).isTrue();
    }

    @Test
    public void whenClickAddCardButton_thenMoveToCameraGallerySelectionActivity() throws Exception {
        // when
        subject.findViewById(R.id.add_card_button).performClick();

        // then
        ShadowActivity shadowActivity = shadowOf(subject);
        assertThat(shadowActivity.getNextStartedActivity().getComponent().getClassName()).isEqualTo(CameraGallerySelectionActivity.class.getCanonicalName());
    }

    @Test
    public void whenLaunched_thenSetShowHideTabButtonSelectedAndChangeOrderButtonUnselected() throws Exception {
        assertThat(subject.showHideTabButton.isSelected()).isTrue();
        assertThat(subject.changeOrderTabButton.isSelected()).isFalse();
    }

    @Test
    public void givenLaunched_whenClickChangeOrderTabButton_thenSetShowHideTabButtonUnSelectedAndChangeOrderButtonSelected() throws Exception {
        // when
        subject.showHideRecyclerView = mock(RecyclerView.class);
        subject.changeOrderTabButton.performClick();

        // then
        assertThat(subject.showHideTabButton.isSelected()).isFalse();
        assertThat(subject.changeOrderTabButton.isSelected()).isTrue();
    }


    @Test
    public void givenLaunched_whenClickChangeOrderTabButton_thenShowChangeOrderRecycleViewAndShowItemMoveIcon() throws Exception {
        // when
        subject.changeOrderTabButton.performClick();

        // then
        assertThat(subject.showHideRecyclerView.getVisibility()).isEqualTo(GONE);
        assertThat(subject.changeOrderRecyclerView.getVisibility()).isEqualTo(View.VISIBLE);
        ImageView itemMoveIcon = ((ImageView) subject.changeOrderRecyclerView.getChildAt(0).findViewById(R.id.item_move_icon));
        assertThat(itemMoveIcon.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void givenChangeOrderTabButtonSelected_whenClickShowHideTabButton_thenSetShowHideTabButtonUnSelectedAndChangeOrderButtonUnselected() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();
        // when
        subject.showHideTabButton.performClick();
        // then
        assertThat(subject.showHideTabButton.isSelected()).isTrue();
        assertThat(subject.changeOrderTabButton.isSelected()).isFalse();
    }

    @Test
    public void givenChangeOrderTabButtonSelected_whenClickShowHideTabButton_thenShowShowAndHideIconAndHideItemMoveIcon() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();
        // when
        subject.showHideTabButton.performClick();
        // then
        ImageView showHideIconView = ((ImageView) subject.showHideRecyclerView.getChildAt(2).findViewById(R.id.show_hide_icon));
        ImageView hideIconView = ((ImageView) subject.showHideRecyclerView.getChildAt(2).findViewById(R.id.hide_icon));
        ImageView itemMoveIcon = ((ImageView) subject.showHideRecyclerView.getChildAt(0).findViewById(R.id.item_move_icon));
        assertThat(showHideIconView.getVisibility()).isEqualTo(GONE);
        assertThat(hideIconView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(itemMoveIcon.getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void givenChangeOrderTabButtonSelected_whenClickOneItemInTheCardList_thenShowSnackBar() throws Exception {
        // given
        subject.changeOrderTabButton.performClick();
        // when
        subject.changeOrderRecyclerView.getChildAt(1).performClick();
        // then
        assertThat(ShadowSnackbar.getLatestSnackbar()).isNotNull();
        assertThat(ShadowSnackbar.getTextOfLatestSnackbar()).isEqualTo("순서를 바꾸시려면 카드를 길게 눌러보세요");
    }

    @Test
    public void givenIntentFromShareCardActivity_whenLaunched_thenShowSnackBar() throws Exception {
        // given
        Intent intent = new Intent(RuntimeEnvironment.application, CardListActivity.class);
        intent.putExtra(ApplicationConstants.INTENT_KEY_SHARE_CARD, true);
        // when
        setupActivityWithIntent(CardListActivity.class, intent);
        // then
        assertThat(ShadowSnackbar.getLatestSnackbar()).isNotNull();
        assertThat(ShadowSnackbar.getTextOfLatestSnackbar()).isEqualTo("공유 받은 카드가 추가되었습니다");
    }

    @Test
    public void whenScrolledBottom_thenGoneAddCardButton() throws Exception {
        subject.showHideRecyclerView.scrollToPosition(subject.showHideRecyclerView.getAdapter().getItemCount()-1);
        assertThat(subject.addCardButton.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void givenLessThen6Cards_whenScrolledBottom_thenVisibleAddCardButton() throws Exception {
        when(cardRepository.getSingleCardListWithCategoryId(applicationManager.getCategoryModel().index)).thenReturn(getSmallCardModelList());
        subject = setupActivity(CardListActivity.class);

        assertThat(subject.addCardButton.getVisibility()).isEqualTo(View.VISIBLE);
        subject.showHideRecyclerView.scrollToPosition(subject.showHideRecyclerView.getChildCount()-1);
        assertThat(subject.addCardButton.getVisibility()).isEqualTo(View.VISIBLE);
    }

    private CategoryModel getCategoryModel() {
        CategoryModel categoryModel = new CategoryModel();
        categoryModel.index = 0;
        categoryModel.title = "먹을 것";
        categoryModel.color = ResourcesUtil.RED;
        return categoryModel;
    }

    private List<CardModel> getCardModelList() {
        List<CardModel> list = Lists.newArrayList();

        String contentFolder = ContentsUtil.getContentFolder(RuntimeEnvironment.application.getApplicationContext()) + File.separator;

        list.add(CardModel.builder().name("물0").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(0).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물1").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(1).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(true).build());
        list.add(CardModel.builder().name("물2").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(2).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(true).build());
        list.add(CardModel.builder().name("물3").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(3).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물4").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(4).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물5").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(5).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("젤리0").contentPath(contentFolder+"haribo.mp4").firstTime("20161018_000003").categoryId(0).cardIndex(6).cardType(CardModel.CardType.VIDEO_CARD).thumbnailPath(contentFolder+"haribo.jpg").hide(false).build());
        list.add(CardModel.builder().name("젤리1").contentPath(contentFolder+"haribo.mp4").firstTime("20161018_000003").categoryId(0).cardIndex(7).cardType(CardModel.CardType.VIDEO_CARD).thumbnailPath(contentFolder+"haribo.jpg").hide(true).build());

        return list;
    }

    private List<CardModel> getSmallCardModelList() {
        List<CardModel> list = Lists.newArrayList();

        String contentFolder = ContentsUtil.getContentFolder(RuntimeEnvironment.application.getApplicationContext()) + File.separator;

        list.add(CardModel.builder().name("물0").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(0).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물1").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(1).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(true).build());
        list.add(CardModel.builder().name("물2").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(2).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(true).build());
        list.add(CardModel.builder().name("물3").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(3).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물4").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(4).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());
        list.add(CardModel.builder().name("물5").contentPath(contentFolder+"water.png").firstTime("20161018_000003").categoryId(0).cardIndex(5).cardType(CardModel.CardType.PHOTO_CARD).thumbnailPath("").hide(false).build());

        return list;
    }
}