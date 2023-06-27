package de.beatsaber.favorites;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Properties;

@Slf4j
public class Utils {

    private static String SONG_CACHE = "song.cache";
    private static final String delimiter = ";;";

    private static Properties cache;
    private static Gson mapper = new Gson();

    public static final String getDefaultDirectory() {
        String OS = System.getProperty("os.name").toUpperCase();
        if (OS.contains("WIN")) {
            return System.getenv("APPDATA");
        } else if (OS.contains("MAC")) {
            return System.getProperty("user.home") + "/Library/Application " + "Support";
        } else if (OS.contains("NUX")) {
            return System.getProperty("user.home");
        }
        return System.getProperty("user.dir");
    }

    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


    public static void readCache() {
        cache = new Properties();
        File f = new File(SONG_CACHE);
        log.error(f.getAbsolutePath());
        try (InputStream input = new FileInputStream(SONG_CACHE)) {
            // load cache file
            cache.load(input);
        } catch (IOException e) {
            log.error("Could not load cache file: " + e.getMessage());
        }
        log.info("Cache loaded.");
    }

    public static void storeCache() {
        try (OutputStream output = new FileOutputStream(SONG_CACHE)) {
            // store cache file
            cache.store(output, null);
        } catch (IOException e) {
            log.error("Could not store cache file: " + e.getMessage());
        }
        log.info("Cache stored.");
    }

    public static RowData getSongCacheEntryAsRowData(String songHash) {
        Object songData = cache.get(songHash);
        if (songData != null) {
            return convertSongDataToRowData(songHash, songData.toString());
        }
        return null;
    }

    private static RowData convertSongDataToRowData(String songHash, String songData) {
        RowData rowData = mapper.fromJson(songData, RowData.class);
        return rowData;
    }

    public static void storeRowDataInCache(String songHash, RowData rowData) {
        String songData = mapper.toJson(rowData);
        cache.put(songHash, songData);
    }
}
