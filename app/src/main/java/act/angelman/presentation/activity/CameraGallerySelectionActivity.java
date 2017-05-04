package act.angelman.presentation.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import javax.inject.Inject;

import act.angelman.AngelmanApplication;
import act.angelman.R;
import act.angelman.presentation.custom.CardTitleLayout;
import act.angelman.presentation.manager.ApplicationConstants;
import act.angelman.presentation.manager.ApplicationManager;
import act.angelman.presentation.util.ResourcesUtil;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraGallerySelectionActivity extends AbstractActivity {

    @Inject
    ApplicationManager applicationManager;

    @BindView(R.id.layout_camera)
    public RelativeLayout cameraCard;

    @BindView(R.id.layout_gallery)
    public RelativeLayout galleryCard;

    @BindView(R.id.layout_video)
    public RelativeLayout videoCard;

    @BindView(R.id.title_container)
    CardTitleLayout titleLayout;

    private static final int SELECT_PICTURE = 1;
    private String editCardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AngelmanApplication) getApplication()).getAngelmanComponent().inject(this);
        ResourcesUtil.setColorTheme(this, applicationManager.getCategoryModelColor());
        setContentView(R.layout.activity_camera_gallery_selection);
        ButterKnife.bind(this);

        applicationManager.setCategoryBackground(
                findViewById(R.id.camera_gallery_selection_container),
                applicationManager.getCategoryModelColor()
        );
        titleLayout.hideCardCountText(true);
        titleLayout.hideListCardButton(true);

        editCardId = getIntent().getStringExtra(ApplicationConstants.EDIT_CARD_ID);
        if(editCardId == null) {
            titleLayout.setCategoryModelTitle(applicationManager.getCategoryModel().title);
        } else {
            titleLayout.setCategoryModelTitle(getString(R.string.card_edit_title));
            ((TextView) findViewById(R.id.camera_start_text)).setText(R.string.edit_content_guide_text);
        }
    }

    @OnClick({R.id.layout_camera})
    public void onClickCamera(View view){
        Intent intent = new Intent(this, Camera2Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(ApplicationConstants.EDIT_CARD_ID, editCardId);
        startActivity(intent);
    }

    @OnClick({R.id.layout_gallery})
    public void onClickGallery(View view){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_picture)), SELECT_PICTURE);
    }

    @OnClick({R.id.layout_video})
    public void onClickVideo(View view){
        Intent intent = new Intent(this, VideoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(ApplicationConstants.EDIT_CARD_ID, editCardId);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            Intent intent = new Intent(CameraGallerySelectionActivity.this, PhotoEditorActivity.class);
            intent.putExtra(ApplicationConstants.IMAGE_PATH_EXTRA, data.getData());
            intent.putExtra(ApplicationConstants.EDIT_CARD_ID, editCardId);
            startActivity(intent);
        }
    }
}
