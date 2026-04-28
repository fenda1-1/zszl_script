package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpsCompat {

    private static final Set<String> FALLBACK_ALLOWED_HOSTS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private static final Set<String> LOGGED_FALLBACK_HOSTS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static volatile boolean installed = false;

    static {
        registerHost("qq.com");
        registerHost("haowallpaper.com");
    }

    private HttpsCompat() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }

        try {
            X509TrustManager delegate = loadDefaultTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new HostScopedFallbackTrustManager(delegate) },
                    new SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            SSLContext.setDefault(sslContext);
            installed = true;
            zszlScriptMod.LOGGER.info("Installed HTTPS compatibility layer for legacy Java trust stores");
        } catch (Throwable t) {
            installed = true;
            zszlScriptMod.LOGGER.warn("Failed to install HTTPS compatibility layer; using JVM defaults", t);
        }
    }

    public static Connection connect(String url) {
        registerHostFromUrl(url);
        return Jsoup.connect(url);
    }

    public static String readText(String url, String userAgent, int timeoutMs) throws java.io.IOException {
        registerHostFromUrl(url);
        URLConnection connection = openConnection(new URL(url));
        connection.setConnectTimeout(Math.max(1000, timeoutMs));
        connection.setReadTimeout(Math.max(1000, timeoutMs));
        connection.setRequestProperty("User-Agent", userAgent == null || userAgent.trim().isEmpty()
                ? SharechainPageParser.MOBILE_USER_AGENT
                : userAgent);
        connection.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");

        if (connection instanceof java.net.HttpURLConnection http) {
            http.setInstanceFollowRedirects(true);
            http.setRequestMethod("GET");
        }

        try (InputStream in = connection.getInputStream()) {
            byte[] bytes = readAllBytes(in);
            Charset charset = resolveCharset(connection.getContentType());
            return new String(bytes, charset);
        } finally {
            if (connection instanceof java.net.HttpURLConnection http) {
                http.disconnect();
            }
        }
    }

    public static URLConnection openConnection(URL url) throws java.io.IOException {
        if (url != null) {
            registerHost(url.getHost());
        }
        return url.openConnection();
    }

    public static void registerHostFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            registerHost(new URL(url).getHost());
        } catch (Exception ignored) {
        }
    }

    public static void registerHost(String host) {
        String normalized = normalizeHost(host);
        if (!normalized.isEmpty()) {
            FALLBACK_ALLOWED_HOSTS.add(normalized);
        }
    }

    private static X509TrustManager loadDefaultTrustManager() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (TrustManager trustManager : tmf.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new IllegalStateException("No X509TrustManager available from default TrustManagerFactory");
    }

    private static boolean isAllowedHost(String host) {
        String normalized = normalizeHost(host);
        if (normalized.isEmpty()) {
            return false;
        }

        for (String allowed : FALLBACK_ALLOWED_HOSTS) {
            if (normalized.equals(allowed) || normalized.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static Charset resolveCharset(String contentType) {
        if (contentType != null) {
            String[] parts = contentType.split(";");
            for (String part : parts) {
                String trimmed = part == null ? "" : part.trim();
                if (!trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    continue;
                }
                String charsetName = trimmed.substring("charset=".length()).trim();
                if (charsetName.startsWith("\"") && charsetName.endsWith("\"") && charsetName.length() > 1) {
                    charsetName = charsetName.substring(1, charsetName.length() - 1);
                }
                try {
                    return Charset.forName(charsetName);
                } catch (Exception ignored) {
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static byte[] readAllBytes(InputStream in) throws java.io.IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static boolean isTrustStorePathFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("pkix")
                        || lower.contains("unable to find valid certification path")
                        || lower.contains("suncertpathbuilderexception")
                        || lower.contains("unable to get local issuer certificate")
                        || lower.contains("trust anchor")
                        || lower.contains("validatorexception")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static void logFallback(String host, X509Certificate[] chain, CertificateException error) {
        String normalized = normalizeHost(host);
        if (!LOGGED_FALLBACK_HOSTS.add(normalized)) {
            return;
        }

        String subject = "unknown";
        if (chain != null && chain.length > 0 && chain[0] != null) {
            subject = chain[0].getSubjectX500Principal().getName();
        }

        zszlScriptMod.LOGGER.warn(
                "Using legacy HTTPS trust fallback for host {} and certificate {} because the runtime trust store rejected the chain: {}",
                normalized, subject, error.getMessage());
    }

    private static String extractHost(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSession handshake = ((SSLSocket) socket).getHandshakeSession();
            if (handshake != null && handshake.getPeerHost() != null) {
                return handshake.getPeerHost();
            }
            SSLSession session = ((SSLSocket) socket).getSession();
            if (session != null && session.getPeerHost() != null) {
                return session.getPeerHost();
            }
        }
        return null;
    }

    private static final class HostScopedFallbackTrustManager extends X509ExtendedTrustManager {

        private final X509TrustManager delegate;

        private HostScopedFallbackTrustManager(X509TrustManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            checkServerTrustedInternal(chain, authType, extractHost(socket));
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                throws CertificateException {
            checkServerTrustedInternal(chain, authType, engine == null ? null : engine.getPeerHost());
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

        private void checkServerTrustedInternal(X509Certificate[] chain, String authType, String host)
                throws CertificateException {
            try {
                delegate.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                if (isAllowedHost(host) && isTrustStorePathFailure(e)) {
                    logFallback(host, chain, e);
                    return;
                }
                throw e;
            }
        }
    }
}
