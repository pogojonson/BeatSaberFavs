package de.beatsaber.favorites;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class MainApp {

    private JLabel statusLabel;
    private DefaultTableModel dataModel;

    private final int poolSize = 10;
    private final ExecutorService pool = Executors.newFixedThreadPool(poolSize);
    private List<RowData> rowDataList = Collections.synchronizedList(new ArrayList<RowData>());
    private final Thread rowDataRunner = new Thread(new RowDataRunner());

    public static void main(String[] args) {
        new MainApp();
    }

    private class RowDataRunner implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (Thread.interrupted()) {
                    return;
                }
                if (rowDataList.size() > 0) {
                    RowData rowData = rowDataList.remove(0);
                    try {
                        dataModel.addRow(new Object[]{rowData.songAuthor, rowData.songName, rowData.songSubName, rowData.mapper, rowData.bsrKey, rowData.rating, rowData.bpm, rowData.difficulties});
                    } catch (NullPointerException npe) {
                        log.warn(String.format("Problem adding rowData '' to table.", rowData));
                    }
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    MainApp() {
        createGUI();
        Utils.readCache();

        File folder = new File(Utils.getDefaultDirectory() + "\\..\\LocalLow\\Hyperbolic Magnetism\\Beat Saber");

        if (!folder.exists()) {
            statusLabel.setText("How 'bout ya shoddy bum starts sum Beet Sabur befor' ya can get sum favs.");
            return;
        }

        rowDataRunner.start();

        File playerData = new File(folder, "PlayerData.dat");
        try (FileReader reader = new FileReader(playerData.getAbsolutePath())) {
            // read the json file
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

            // get a String from the JSON object
            JSONArray localPlayers = (JSONArray) jsonObject.get("localPlayers");
            localPlayers.forEach(localPlayer ->
            {
                JSONArray favoritesLevelIds = (JSONArray) ((JSONObject) localPlayer).get("favoritesLevelIds");
                favoritesLevelIds.forEach(_levelId ->
                {
                    JSONParser innerJsonParser = new JSONParser();
                    String levelId = ("" + _levelId);
                    String songHash = levelId.startsWith("custom_level_") ? levelId.substring("custom_level_".length()) : levelId;

                    RowData rowData = Utils.getSongCacheEntryAsRowData(songHash);
                    if (rowData == null) {
                        try {
                            URLConnection conn = new URL("https://beatsaver.com/api/maps/by-hash/" + songHash).openConnection();
                            conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
                            try (InputStream is = conn.getInputStream()) {
                                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                                String jsonText = Utils.readAll(rd);
                                JSONObject json = (JSONObject) innerJsonParser.parse(jsonText);
                                rowData = parseJsonToRowData(json);
                                Utils.storeRowDataInCache(songHash, rowData);
                            } catch (ParseException e) {
                                log.error(e.getMessage(), e);
                            }
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    rowDataList.add(rowData);
                });
            });
            statusLabel.setText(String.format("Finished loading '%d' favorites.", dataModel.getRowCount()));
        } catch (IOException | ParseException e) {
            statusLabel.setText("Yer mate might wanna check the log. " + e.getClass().getSimpleName());
            log.error(e.getMessage(), e);
        }
    }

    private RowData parseJsonToRowData(JSONObject json) {
        String bsrKey = json.get("key").toString();

        JSONObject metadata = (JSONObject) json.get("metadata");
        String mapper = metadata.get("levelAuthorName").toString();
        String songAuthorName = metadata.get("songAuthorName").toString();
        String songName = metadata.get("songName").toString();
        String songSubName = metadata.get("songSubName").toString();
        long bpm = (long) Float.parseFloat(metadata.get("bpm").toString());

        JSONObject difficulties = (JSONObject) metadata.get("difficulties");
        List<String> diffs = parseJsonDifficultiesToList(difficulties);

        JSONObject stats = (JSONObject) json.get("stats");
        float rating = Float.parseFloat(stats.get("rating").toString());

        RowData rowData = new RowData(songAuthorName, songName, songSubName, mapper, bsrKey, bpm, rating, String.join(", ", diffs));
        return rowData;
    }

    private class EigeneKlasse {
        public int a;

        public EigeneKlasse(int a) {
            this.a = a;
        }

        public int plus(int b) {
            return a + b;
        }
    }

    private List<String> parseJsonDifficultiesToList(JSONObject difficulties) {
        List<String> diffs = new ArrayList<>();
        int i = 1;
        float f = 1.0f;
        double d = 2.0d;
        long l = 1111111111111111L;
        boolean b = true;
        char c = 'a';
        int bb = 0xFF;
        byte bbb = 127;

        EigeneKlasse lul1 = new EigeneKlasse(1);
        EigeneKlasse lul2 = new EigeneKlasse(2);
        long temp = l + lul1.plus(2);

        if (Boolean.parseBoolean(difficulties.get("easy").toString())) {
            diffs.add("Easy");
        }
        if (Boolean.parseBoolean(difficulties.get("normal").toString())) {
            diffs.add("Normal");
        }
        if (Boolean.parseBoolean(difficulties.get("hard").toString())) {
            diffs.add("Hard");
        }
        if (Boolean.parseBoolean(difficulties.get("expert").toString())) {
            diffs.add("Expert");
        }
        if (Boolean.parseBoolean(difficulties.get("expertPlus").toString())) {
            diffs.add("Expert+");
        }
        return diffs;
    }

    private void createGUI() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("BeatSaber Fav Viewer");
        frame.setBounds(1, 1, 700, 300);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownThreads();
                Utils.storeCache();
            }
        });

        JTable dataTable = new JTable();
        dataTable.setBounds(30, 40, 800, 300);
        dataModel = new DefaultTableModel();
        dataModel.addColumn("Song Author");
        dataModel.addColumn("Song Name");
        dataModel.addColumn("Song SubName");
        dataModel.addColumn("Mapper");
        dataModel.addColumn("Request Id");
        dataModel.addColumn("Rating");
        dataModel.addColumn("bpm");
        dataModel.addColumn("Difficulties");
        dataTable.setModel(dataModel);

        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                String bsrText = "!bsr " + dataTable.getValueAt(dataTable.getSelectedRow(), 4).toString();
                StringSelection stringSelection = new StringSelection(bsrText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                statusLabel.setText(String.format("Copied '%s' to clipboard.", bsrText));
            }
        });

        JScrollPane panel = new JScrollPane(dataTable);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        frame.add(panel, BorderLayout.CENTER);

        statusLabel = new JLabel("Started");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }

    private void shutdownThreads() {
        pool.shutdown();
        rowDataRunner.interrupt();
    }
}
