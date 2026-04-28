package com.zszl.zszlScriptMod.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MailHelper {

    public static final MailHelper INSTANCE = new MailHelper();

    public static final int BTN_RECEIVE_ALL_ID = 43001;
    public static final int BTN_DELETE_ALL_ID = 43002;
    public static final int BTN_VIEW_IDS = 43003;
    public static final int BTN_SETTINGS_ID = 43004;
    public static final int BTN_STOP_ID = 43005;

    public boolean isMailContextActive = false;
    public boolean isFingerprintTicketValid = false;
    public final List<Integer> mailListButtonIds = new ArrayList<>();
    public final List<MailInfo> mailInfoList = new ArrayList<>();

    private boolean waitingForFinalGuiClose = false;
    private boolean waitingForAttachmentId = false;

    public static final class MailInfo {
        public String mailId = "";
        public String title = "";
    }

    private MailHelper() {
    }

    public void deactivateMailContext(String reason) {
        isMailContextActive = false;
    }

    public void stopAutomation(String reason) {
    }

    public void startAutoReceiveAll() {
    }

    public void startAutoDeleteAll() {
    }

    public void onMailSubPageOpened() {
    }

    public void syncCapturedValuesFromRules() {
    }

    public void reset() {
        isMailContextActive = false;
        isFingerprintTicketValid = false;
        waitingForFinalGuiClose = false;
        waitingForAttachmentId = false;
        mailListButtonIds.clear();
        mailInfoList.clear();
    }

    public void onOutboundOwlViewPacket(String channel, byte[] outboundData) {
    }

    public void onViewGuiRemoved(int componentId) {
    }

    public boolean isWaitingForFinalGuiClose() {
        return waitingForFinalGuiClose;
    }

    public void onFinalGuiClosed() {
        waitingForFinalGuiClose = false;
    }

    public void onCreateButtonCaptured(int componentId, String name) {
    }

    public void onCreateLabelCaptured(int componentId, String name) {
    }

    public boolean onMailLabelTextCaptured(int componentId, String text, boolean fallback, boolean replace) {
        return false;
    }

    public void onRemovedComponentCaptured(int removedId) {
    }

    public void onGuiSetHideCaptured(boolean p1, boolean p2, boolean p3) {
    }

    public boolean isWaitingForAttachmentId() {
        return waitingForAttachmentId;
    }

    public void onAttachmentIdCaptured(int componentId) {
        waitingForAttachmentId = false;
    }

    public void onMailViewGuiOpenDetected(int componentId) {
    }

    public void removeMailById(String mailId) {
    }

    public void clearAllMails() {
        mailInfoList.clear();
    }
}
