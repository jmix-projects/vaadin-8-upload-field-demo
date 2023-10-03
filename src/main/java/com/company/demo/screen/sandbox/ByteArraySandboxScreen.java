package com.company.demo.screen.sandbox;

import com.vaadin.ui.Layout;
import com.vaadin.ui.Upload;
import io.jmix.ui.Notifications;
import io.jmix.ui.screen.Screen;
import io.jmix.ui.screen.Subscribe;
import io.jmix.ui.screen.UiController;
import io.jmix.ui.screen.UiDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@UiController("ByteArraySandboxScreen")
@UiDescriptor("byte-array-sandbox-screen.xml")
public class ByteArraySandboxScreen extends Screen {

    private static final Logger log = LoggerFactory.getLogger(ByteArraySandboxScreen.class);

    @Autowired
    private Notifications notifications;

    private ByteArrayOutputStream outputStream;

    private Upload upload;

    @Subscribe
    public void onInit(final InitEvent event) {
        upload = new Upload();

        upload.setReceiver(this::receiveUpload);
        upload.addSucceededListener(this::onSucceed);
        upload.addProgressListener(this::onProgress);

        getWindow().unwrap(Layout.class).addComponent(upload);
    }

    private OutputStream receiveUpload(String filename, String mimeType) {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    private void onSucceed(Upload.SucceededEvent event) {
        // The 'outputStream' object can be processed here
        notifications.create()
                .withCaption("Byte Array Size: " + outputStream.size())
                .show();
    }

    private void onProgress(long readBytes, long contentLength) {
        log.info("Progress - readBytes: {}; contentLength: {}", readBytes, contentLength);
    }
}