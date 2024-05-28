//package com.fongmi.android.tv.player;
//
//import android.content.ContentResolver;
//import android.content.Context;
//import android.net.Uri;
//
//import androidx.annotation.Nullable;
//import androidx.media3.common.util.Assertions;
//import androidx.media3.common.util.Log;
//import androidx.media3.common.util.UnstableApi;
//import androidx.media3.common.util.Util;
//import androidx.media3.datasource.AssetDataSource;
//import androidx.media3.datasource.ContentDataSource;
//import androidx.media3.datasource.DataSchemeDataSource;
//import androidx.media3.datasource.DataSource;
//import androidx.media3.datasource.DataSpec;
//import androidx.media3.datasource.DefaultHttpDataSource;
//import androidx.media3.datasource.FileDataSource;
//import androidx.media3.datasource.RawResourceDataSource;
//import androidx.media3.datasource.TransferListener;
//import androidx.media3.datasource.UdpDataSource;
//
//import com.fongmi.android.tv.utils.CutM3u8Ads;
//import com.iheartradio.m3u8.ParseException;
//import com.iheartradio.m3u8.PlaylistException;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URISyntaxException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
///**
// * 抄的DefaultDatasource
// * 加了去广告处理
// */
//public final class CutAdsDataSource implements DataSource {
//    private InputStream inputStream = null;
//
//    public static final class Factory implements DataSource.Factory {
//
//        private final Context context;
//        private final DataSource.Factory baseDataSourceFactory;
//        private final String fromSource;
//        @Nullable
//        private TransferListener transferListener;
//
//        /**
//         * Creates an instance.
//         *
//         * @param context A context.
//         */
//
//        public Factory(Context context, DataSource.Factory baseDataSourceFactory, String fromSource) {
//            this.context = context.getApplicationContext();
//            this.baseDataSourceFactory = baseDataSourceFactory;
//            this.fromSource = fromSource;
//        }
//
//        @UnstableApi
//        @Override
//        public CutAdsDataSource createDataSource() {
//            CutAdsDataSource dataSource =
//                    new CutAdsDataSource(context, baseDataSourceFactory.createDataSource());
//            if (transferListener != null) {
//                dataSource.addTransferListener(transferListener);
//            }
//            if (fromSource != null)
//                dataSource.addFromSource(fromSource);
//            return dataSource;
//        }
//
//
//    }
//
//    private static final String TAG = "CutAdsDataSource";
//
//    private static final String SCHEME_ASSET = "asset";
//    private static final String SCHEME_CONTENT = "content";
//    private static final String SCHEME_RTMP = "rtmp";
//    private static final String SCHEME_UDP = "udp";
//    private static final String SCHEME_DATA = DataSchemeDataSource.SCHEME_DATA;
//    private static final String SCHEME_RAW = RawResourceDataSource.RAW_RESOURCE_SCHEME;
//    private static final String SCHEME_ANDROID_RESOURCE = ContentResolver.SCHEME_ANDROID_RESOURCE;
//
//    private final Context context;
//    private final List<TransferListener> transferListeners;
//    private final DataSource baseDataSource;
//    @Nullable
//    private String fromSource;
//
//    // Lazily initialized.
//    @Nullable
//    private DataSource fileDataSource;
//    @Nullable
//    private DataSource assetDataSource;
//    @Nullable
//    private DataSource contentDataSource;
//    @Nullable
//    private DataSource rtmpDataSource;
//    @Nullable
//    private DataSource udpDataSource;
//    @Nullable
//    private DataSource dataSchemeDataSource;
//    @Nullable
//    private DataSource rawResourceDataSource;
//
//    @Nullable
//    private DataSource dataSource;
//
//    /**
//     * Constructs a new instance, optionally configured to follow cross-protocol redirects.
//     *
//     * @param context                     A context.
//     * @param allowCrossProtocolRedirects Whether to allow cross-protocol redirects.
//     */
//    @UnstableApi
//    public CutAdsDataSource(Context context, boolean allowCrossProtocolRedirects) {
//        this(
//                context,
//                /* userAgent= */ null,
//                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
//                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
//                allowCrossProtocolRedirects);
//    }
//
//    @UnstableApi
//    public CutAdsDataSource(
//            Context context, @Nullable String userAgent, boolean allowCrossProtocolRedirects) {
//        this(
//                context,
//                userAgent,
//                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
//                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
//                allowCrossProtocolRedirects);
//    }
//
//    @UnstableApi
//    public CutAdsDataSource(
//            Context context,
//            @Nullable String userAgent,
//            int connectTimeoutMillis,
//            int readTimeoutMillis,
//            boolean allowCrossProtocolRedirects) {
//        this(
//                context,
//                new DefaultHttpDataSource.Factory()
//                        .setUserAgent(userAgent)
//                        .setConnectTimeoutMs(connectTimeoutMillis)
//                        .setReadTimeoutMs(readTimeoutMillis)
//                        .setAllowCrossProtocolRedirects(allowCrossProtocolRedirects)
//                        .createDataSource());
//    }
//
//    @UnstableApi
//    public CutAdsDataSource(Context context, DataSource baseDataSource) {
//        this.context = context.getApplicationContext();
//        this.baseDataSource = Assertions.checkNotNull(baseDataSource);
//        transferListeners = new ArrayList<>();
//    }
//
//    @UnstableApi
//    @Override
//    public void addTransferListener(TransferListener transferListener) {
//        Assertions.checkNotNull(transferListener);
//        baseDataSource.addTransferListener(transferListener);
//        transferListeners.add(transferListener);
//        maybeAddListenerToDataSource(fileDataSource, transferListener);
//        maybeAddListenerToDataSource(assetDataSource, transferListener);
//        maybeAddListenerToDataSource(contentDataSource, transferListener);
//        maybeAddListenerToDataSource(rtmpDataSource, transferListener);
//        maybeAddListenerToDataSource(udpDataSource, transferListener);
//        maybeAddListenerToDataSource(dataSchemeDataSource, transferListener);
//        maybeAddListenerToDataSource(rawResourceDataSource, transferListener);
//    }
//
//    @UnstableApi
//    public void addFromSource(String fromSource) {
//        Assertions.checkNotNull(fromSource);
//        this.fromSource = fromSource;
//    }
//
//    @UnstableApi
//    @Override
//    public long open(DataSpec dataSpec) throws IOException {
//        Assertions.checkState(dataSource == null);
//        // Choose the correct source for the scheme.
//        String scheme = dataSpec.uri.getScheme();
//        if (Util.isLocalFileUri(dataSpec.uri)) {
//            String uriPath = dataSpec.uri.getPath();
//            if (uriPath != null && uriPath.startsWith("/android_asset/")) {
//                dataSource = getAssetDataSource();
//            } else {
//                dataSource = getFileDataSource();
//            }
//        } else if (SCHEME_ASSET.equals(scheme)) {
//            dataSource = getAssetDataSource();
//        } else if (SCHEME_CONTENT.equals(scheme)) {
//            dataSource = getContentDataSource();
//        } else if (SCHEME_RTMP.equals(scheme)) {
//            dataSource = getRtmpDataSource();
//        } else if (SCHEME_UDP.equals(scheme)) {
//            dataSource = getUdpDataSource();
//        } else if (SCHEME_DATA.equals(scheme)) {
//            dataSource = getDataSchemeDataSource();
//        } else if (SCHEME_RAW.equals(scheme) || SCHEME_ANDROID_RESOURCE.equals(scheme)) {
//            dataSource = getRawResourceDataSource();
//        } else {
//            dataSource = baseDataSource;
//        }
//        long length = dataSource.open(dataSpec);
//
//        // 处理量子和非凡的插播广告
//        if (isLzOrFf(dataSpec.uri.toString())) {
//            length = modifyM3u8();
//        }
//        return length;
//    }
//
//    private long modifyM3u8() throws IOException {
//
//        // 读取基础数据源内容到字节数组
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int bytesRead;
//        while ((bytesRead = baseDataSource.read(buffer, 0, buffer.length)) != -1) {
//            byteArrayOutputStream.write(buffer, 0, bytesRead);
//        }
//        byte[] data = byteArrayOutputStream.toByteArray();
//        try {
//            data = CutM3u8Ads.cutAds(data, null);
//        } catch (PlaylistException | ParseException exception) {
//            Log.e(TAG, exception.toString());
//            throw new RuntimeException();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//        inputStream = new ByteArrayInputStream(data);
//        return data.length;
//    }
//
//    /**
//     * 处理量子和非凡的广告
//     *
//     * @param url
//     * @return
//     */
//    private boolean isLzOrFf(String url) {
//        return url != null && url.toLowerCase().endsWith(".m3u8") && haveAds();
//    }
//
//    private boolean haveAds() {
//        return fromSource == null ||
//                fromSource.equals("lzm3u8") ||
//                fromSource.equals("ffm3u8") ||
//                fromSource.equals("bfzym3u8");
//    }
//    @UnstableApi
//    @Override
//    public int read(byte[] buffer, int offset, int length) throws IOException {
//        return inputStream != null ? inputStream.read(buffer, offset, length) :
//                Assertions.checkNotNull(dataSource).read(buffer, offset, length);
//    }
//
//    @UnstableApi
//    @Override
//    @Nullable
//    public Uri getUri() {
//        return dataSource == null ? null : dataSource.getUri();
//    }
//
//    @UnstableApi
//    @Override
//    public Map<String, List<String>> getResponseHeaders() {
//        return dataSource == null ? Collections.emptyMap() : dataSource.getResponseHeaders();
//    }
//
//    @UnstableApi
//    @Override
//    public void close() throws IOException {
//        if (dataSource != null) {
//            try {
//                dataSource.close();
//            } finally {
//                dataSource = null;
//            }
//        }
//        if (inputStream != null) {
//            inputStream.close();
//        }
//    }
//
//    private DataSource getUdpDataSource() {
//        if (udpDataSource == null) {
//            udpDataSource = new UdpDataSource();
//            addListenersToDataSource(udpDataSource);
//        }
//        return udpDataSource;
//    }
//
//    private DataSource getFileDataSource() {
//        if (fileDataSource == null) {
//            fileDataSource = new FileDataSource();
//            addListenersToDataSource(fileDataSource);
//        }
//        return fileDataSource;
//    }
//
//    private DataSource getAssetDataSource() {
//        if (assetDataSource == null) {
//            assetDataSource = new AssetDataSource(context);
//            addListenersToDataSource(assetDataSource);
//        }
//        return assetDataSource;
//    }
//
//    private DataSource getContentDataSource() {
//        if (contentDataSource == null) {
//            contentDataSource = new ContentDataSource(context);
//            addListenersToDataSource(contentDataSource);
//        }
//        return contentDataSource;
//    }
//
//    private DataSource getRtmpDataSource() {
//        if (rtmpDataSource == null) {
//            try {
//                Class<?> clazz = Class.forName("androidx.media3.datasource.rtmp.RtmpDataSource");
//                rtmpDataSource = (DataSource) clazz.getConstructor().newInstance();
//                addListenersToDataSource(rtmpDataSource);
//            } catch (ClassNotFoundException e) {
//                // Expected if the app was built without the RTMP extension.
//                Log.w(TAG, "Attempting to play RTMP stream without depending on the RTMP extension");
//            } catch (Exception e) {
//                // The RTMP extension is present, but instantiation failed.
//                throw new RuntimeException("Error instantiating RTMP extension", e);
//            }
//            if (rtmpDataSource == null) {
//                rtmpDataSource = baseDataSource;
//            }
//        }
//        return rtmpDataSource;
//    }
//
//    private DataSource getDataSchemeDataSource() {
//        if (dataSchemeDataSource == null) {
//            dataSchemeDataSource = new DataSchemeDataSource();
//            addListenersToDataSource(dataSchemeDataSource);
//        }
//        return dataSchemeDataSource;
//    }
//
//    private DataSource getRawResourceDataSource() {
//        if (rawResourceDataSource == null) {
//            rawResourceDataSource = new RawResourceDataSource(context);
//            addListenersToDataSource(rawResourceDataSource);
//        }
//        return rawResourceDataSource;
//    }
//
//    private void addListenersToDataSource(DataSource dataSource) {
//        for (int i = 0; i < transferListeners.size(); i++) {
//            dataSource.addTransferListener(transferListeners.get(i));
//        }
//    }
//
//    private void maybeAddListenerToDataSource(
//            @Nullable DataSource dataSource, TransferListener listener) {
//        if (dataSource != null) {
//            dataSource.addTransferListener(listener);
//        }
//    }
//}
