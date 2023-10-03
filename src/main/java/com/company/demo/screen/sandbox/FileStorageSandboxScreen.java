package com.company.demo.screen.sandbox;

import com.vaadin.ui.Layout;
import com.vaadin.ui.Upload;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FileStorageException;
import io.jmix.ui.Notifications;
import io.jmix.ui.Notifications.NotificationType;
import io.jmix.ui.UiComponentProperties;
import io.jmix.ui.component.HBoxLayout;
import io.jmix.ui.component.Label;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import io.jmix.ui.upload.TemporaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.UUID;

@UiController("FileStorageSandboxScreen")
@UiDescriptor("file-storage-sandbox-screen.xml")
public class FileStorageSandboxScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(FileStorageSandboxScreen.class);
    private static final int BYTES_IN_MEGABYTE = 1048576;

    @Autowired
    private Label<String> fileNameLabel;
    @Autowired
    private HBoxLayout uploadWrapper;

    @Autowired
    private Notifications notifications;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private UiComponentProperties uiComponentProperties;

    private String fileName;
    private UUID fileId;
    private UUID tempFileId;

    private Upload upload;

    @Subscribe
    public void onInit(final InitEvent event) {
        upload = new Upload();

//        upload.setImmediateMode(false);
//        upload.setAcceptMimeTypes("image/png");

        upload.setReceiver(this::receiveUpload);
        upload.addStartedListener(this::onStart);
        upload.addFinishedListener(this::onFinish);
        upload.addChangeListener(this::onChange);
        upload.addSucceededListener(this::onSucceed);
        upload.addFailedListener(this::onFail);
        upload.addProgressListener(this::onProgress);

        uploadWrapper.unwrap(Layout.class).addComponent(upload);
    }

    /**
     * The upload component writes the received data to an java.io.OutputStream.
     */
    private OutputStream receiveUpload(String filename, String mimeType) {
        try {
            TemporaryStorage.FileInfo fileInfo = temporaryStorage.createFile();
            tempFileId = fileInfo.getId();
            File tmpFile = fileInfo.getFile();

            return new FileOutputStream(tmpFile);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to receive file '%s' of MIME type: %s", fileName, mimeType), e);
        }
    }

    /**
     * Upload.StartedEvent event is sent when the upload is started to received.
     */
    private void onStart(Upload.StartedEvent event) {
        notifications.create(NotificationType.TRAY)
                .withCaption("StartedEvent")
                .withDescription("File Name: " + event.getFilename() +
                        "\nMIMEType: " + event.getMIMEType() +
                        "\nContentLength: " + event.getContentLength())
                .show();

        // Example of handling file size limit
        if (event.getContentLength() > getActualFileSizeLimit()) {
            upload.interruptUpload();
            notifications.create(NotificationType.TRAY)
                    .withCaption(String.format("Upload interrupted: file '%s' is too big", event.getFilename()))
                    .show();
        }
    }

    /**
     * Upload.FinishedEvent is sent when the upload receives a file, regardless
     * of whether the reception was successful or failed. If you wish to
     * distinguish between the two cases, use either SucceededEvent or
     * FailedEvent, which are both subclasses of the FinishedEvent.
     */
    private void onFinish(Upload.FinishedEvent event) {
        notifications.create(NotificationType.TRAY)
                .withCaption("FinishedEvent")
                .withDescription("File Name: " + event.getFilename() +
                        "\nMIMEType: " + event.getMIMEType() +
                        "\nLength: " + event.getLength())
                .show();
    }

    /**
     * Upload.ChangeEvent event is sent when the value (filename) of the upload changes.
     */
    private void onChange(Upload.ChangeEvent event) {
        notifications.create(NotificationType.TRAY)
                .withCaption("ChangeEvent")
                .withDescription("File Name: " + event.getFilename())
                .show();
    }

    /**
     * Upload.SucceededEvent event is sent when the upload is received successfully.
     */
    private void onSucceed(Upload.SucceededEvent event) {
        notifications.create(NotificationType.TRAY)
                .withCaption("SucceededEvent")
                .withDescription("File Name: " + event.getFilename() +
                        "\nMIMEType: " + event.getMIMEType() +
                        "\nLength: " + event.getLength())
                .show();

        fileName = event.getFilename();
        fileId = tempFileId;

        saveFile(fileName);
        fileNameLabel.setValue(fileName);
    }

    protected void saveFile(String fileName) {
        FileRef fileRef = temporaryStorage.putFileIntoStorage(fileId, fileName, fileStorage);
        // TODO: gg, set 'fileRef' to entity
        notifications.create(NotificationType.TRAY)
                .withCaption("Saved to File Storage")
                .show();
    }

    /**
     * Upload.FailedEvent event is sent when the upload is received, but the
     * reception is interrupted for some reason.
     */
    private void onFail(Upload.FailedEvent event) {
        notifications.create(NotificationType.TRAY)
                .withCaption("FailedEvent")
                .withDescription("File Name: " + event.getFilename() +
                        "\nMIMEType: " + event.getMIMEType() +
                        "\nLength: " + event.getLength() +
                        "\nReason: " + event.getReason())
                .show();


        try {
            temporaryStorage.deleteFile(tempFileId);
            tempFileId = null;
        } catch (Exception e) {
            if (e instanceof FileStorageException) {
                FileStorageException fse = (FileStorageException) e;
                if (fse.getType() != FileStorageException.Type.FILE_NOT_FOUND) {
                    log.warn(String.format("Could not remove temp file %s after broken uploading", tempFileId));
                }
            }
            log.warn(String.format("Error while delete temp file %s", tempFileId));
        }
    }

    private void onProgress(long readBytes, long contentLength) {
        log.info("Progress - readBytes: {}; contentLength: {}", readBytes, contentLength);
    }

    protected long getActualFileSizeLimit() {
        int maxUploadSizeMb = uiComponentProperties.getUploadFieldMaxUploadSizeMb();
        return (long) maxUploadSizeMb * BYTES_IN_MEGABYTE;
    }
}