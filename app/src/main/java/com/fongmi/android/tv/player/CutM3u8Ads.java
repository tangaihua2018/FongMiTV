package com.fongmi.android.tv.player;

import static com.fongmi.android.tv.utils.DialogUtils.showToast;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;

import com.fongmi.android.tv.App;
import com.iheartradio.m3u8.Encoding;
import com.iheartradio.m3u8.Format;
import com.iheartradio.m3u8.ParseException;
import com.iheartradio.m3u8.PlaylistException;
import com.iheartradio.m3u8.PlaylistParser;
import com.iheartradio.m3u8.PlaylistWriter;
import com.iheartradio.m3u8.data.MediaPlaylist;
import com.iheartradio.m3u8.data.Playlist;
import com.iheartradio.m3u8.data.TrackData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CutM3u8Ads {
    public static byte[] cutAds(byte[] m3u8In) throws PlaylistException, IOException, ParseException {
        Handler handler = new Handler(Looper.getMainLooper());

        InputStream inputStream = new ByteArrayInputStream(m3u8In);
        PlaylistParser parser = new PlaylistParser(inputStream, Format.EXT_M3U, Encoding.UTF_8);
        Playlist playlist = parser.parse();
        MediaPlaylist mediaPlaylist = playlist.getMediaPlaylist();

        if (mediaPlaylist == null) return m3u8In;

        double duration = cut_ads_list(mediaPlaylist);
        if (duration> 0) {
            @SuppressLint("DefaultLocale")
            String text = String.format("已经去掉插播广告，总时间：%.03f秒", duration);
            handler.post(() -> showToast(App.get(), text));
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PlaylistWriter writer = new PlaylistWriter(outputStream, Format.EXT_M3U, Encoding.UTF_8);
        writer.write(playlist);

        return outputStream.toByteArray();
    }

    public static double cut_ads_list(MediaPlaylist mediaPlaylist) {
        List<TrackData> trackDataList = mediaPlaylist.getTracks();
        boolean start = false;
        int cnt = 0, len = trackDataList.size();
        for (int i = 0; i < len; i++) {
            TrackData trackData = trackDataList.get(i);
            if (trackData.hasDiscontinuity() && !start && i > 0) {
                String prevUrl = incrementTsFilename(trackDataList.get(i - 1).getUri());
                if (!trackData.getUri().equals(prevUrl) && cnt == 0) {
                    start = true;
                    cnt += 0;
                }
            } else if (trackData.hasDiscontinuity() && start) {
                start = false;
            }
            trackData.setDiscontinuity(start);
        }

        double duration = 0;
        for (int i = 0; i < len; ) {
            if (trackDataList.get(i).hasDiscontinuity()) {
                trackDataList.remove(i);
                duration += trackDataList.get(i).getTrackInfo().duration;
                len--;
            } else {
                i++;
            }
        }

        return duration;
    }

    public static String incrementTsFilename(String filename) {
        Pattern pattern = Pattern.compile("(\\d+)(?=\\.ts$)");
        Matcher matcher = pattern.matcher(filename);

        if (matcher.find()) {
            String number = matcher.group(1);
            long newNumber = Long.parseLong(number) + 1;
            String newNumberStr = String.format("%0" + number.length() + "d", newNumber);
            String newFilename = filename.substring(0, matcher.start(1)) + newNumberStr + filename.substring(matcher.end(1));
            return newFilename;
        } else {
            return filename;
        }
    }

}
