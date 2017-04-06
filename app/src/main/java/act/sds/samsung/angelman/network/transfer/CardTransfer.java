package act.sds.samsung.angelman.network.transfer;


import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import act.sds.samsung.angelman.AngelmanApplication;
import act.sds.samsung.angelman.domain.model.CardModel;
import act.sds.samsung.angelman.domain.model.CardTransferModel;
import act.sds.samsung.angelman.presentation.listener.OnDownloadCompleteListener;
import act.sds.samsung.angelman.presentation.util.ContentsUtil;
import act.sds.samsung.angelman.presentation.util.FileUtil;

public class CardTransfer {


    private static final String DEFAULT_SHARE_REFERENCE = "share";
    private static final String IMAGE_FILE_EXTENSION = ".jpg";

    private DatabaseReference shareDataReference;
    private StorageReference storageReference;
    private FirebaseStorage storage;
    private Context context;

    public CardTransfer(Context context) {
        this.context = context;
        ((AngelmanApplication) context).getAngelmanComponent().inject(this);
        storage = FirebaseStorage.getInstance();
        shareDataReference = FirebaseDatabase.getInstance().getReference(DEFAULT_SHARE_REFERENCE);
        storageReference = storage.getReference(DEFAULT_SHARE_REFERENCE);
    }

    public void uploadCard(final CardModel cardModel, final OnSuccessListener<Map<String, String>> onSuccessListener, final OnFailureListener onFailureListener) {

        final String key = generateShareKey();


        final String zipFilePath = makeShareZipFile(cardModel, key);

        storageReference.child(key).child(key + ".zip").putFile(Uri.fromFile(new File(zipFilePath))) // Zip File upload
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Uri uploadFileUri = getUploadFileUri(cardModel);

                        storageReference.child(key).child(key + IMAGE_FILE_EXTENSION).putFile(uploadFileUri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        Map<String, String> resultMap = Maps.newHashMap();
                                        resultMap.put("key", key);
                                        resultMap.put("url", taskSnapshot.getDownloadUrl() == null ? "" : taskSnapshot.getDownloadUrl().toString());
                                        onSuccessListener.onSuccess(resultMap);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                onFailureListener.onFailure(e);
                            }
                        });
                        shareDataReference.child(key).child("cardType").setValue(cardModel.cardType.getValue());
                        shareDataReference.child(key).child("name").setValue(cardModel.name);
                        shareDataReference.child(key).child("contentPath").setValue(taskSnapshot.getDownloadUrl() == null ? "" : taskSnapshot.getDownloadUrl().toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {
                onFailureListener.onFailure(e);
            }
        });

    }

    public void downloadCard(String receiveKey, final OnDownloadCompleteListener downloadCompleteListener) {
        shareDataReference.child(receiveKey).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final CardTransferModel cardTransferModel = new CardTransferModel();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            setCardModealData(cardTransferModel, snapshot);
                        }

                        final File localFile = new File(ContentsUtil.getTempFolder() + File.separator + "temp.zip");
                        storage.getReferenceFromUrl(cardTransferModel.contentPath)
                                .getFile(localFile)
                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        downloadCompleteListener.onSuccess(cardTransferModel, localFile.getAbsolutePath());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d("DOWNLOAD FAIL ", "download fail.");
                                        downloadCompleteListener.onFail();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("onCancelled", databaseError.getMessage());
                        downloadCompleteListener.onFail();
                    }
                }
        );
    }

    private Uri getUploadFileUri(CardModel cardModel) {
        Uri uploadFileUri;
        if (cardModel.cardType == CardModel.CardType.PHOTO_CARD) {
            uploadFileUri = Uri.fromFile(new File(getAbsoluteContentsPath(cardModel.contentPath)));
        } else {
            uploadFileUri = Uri.fromFile(new File(getAbsoluteContentsPath(cardModel.thumbnailPath)));
        }
        return uploadFileUri;
    }

    @NonNull
    private String makeShareZipFile(CardModel cardModel, String key) {
        List<String> filePathList = Lists.newArrayList(getAbsoluteContentsPath(cardModel.contentPath));

        if (!Strings.isNullOrEmpty(cardModel.voicePath) && getAbsoluteContentsPath(cardModel.voicePath) != null) {
            filePathList.add(getAbsoluteContentsPath(cardModel.voicePath));
        }
        if (cardModel.cardType == CardModel.CardType.VIDEO_CARD) {
            filePathList.add(getAbsoluteContentsPath(cardModel.thumbnailPath));
        }

        final String zipFilePath = context.getCacheDir() + File.separator + key + ".zip";
        try {
            FileUtil.zip(filePathList.toArray(new String[filePathList.size()]), zipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipFilePath;
    }

    private String getAbsoluteContentsPath(String filePath) {
        File contentFile = ContentsUtil.getContentFile(filePath);
        if(contentFile != null){
            return ContentsUtil.getContentFile(filePath).getAbsolutePath();
        }else{
            return null;
        }
    }

    private void setCardModealData(CardTransferModel newCardModel, DataSnapshot snapshot) {
        if (snapshot.getKey().equals("name")) {
            newCardModel.name = snapshot.getValue().toString();
        } else if (snapshot.getKey().equals("contentPath")) {
            newCardModel.contentPath = snapshot.getValue().toString();
        } else if (snapshot.getKey().equals("cardType")) {
            newCardModel.cardType = snapshot.getValue().toString();
        }
    }

    // TODO : deviceID unique key 확인
    private String generateShareKey() {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId + System.currentTimeMillis();
    }

}