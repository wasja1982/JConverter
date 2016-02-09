package AtomM;

import AtomM.GUI.DLog;
import AtomM.GUI.FConverter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class TConverter extends Thread {
    private DLog log;
    private FConverter form;

    public TConverter(DLog log, FConverter form) {
        this.log = log;
        this.form = form;
    }

    private void createFile(String filename, ArrayList data) {
        try {
            OutputStreamWriter buf = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
            for (int i = 0; i < data.size(); i++) {
                Object item = data.get(i);
                if (item == null) {
                    continue;
                }
                String str = item instanceof String ? (String) item : item.toString();
                buf.write(str + "\r\n");
            }
            buf.flush();
            buf.close();
        }
        catch (Exception e) {
            log.println( "File \"" + filename + "\" not saved (" + e.getMessage() + ")." );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        boolean parseAll = form.getParseAll();
        boolean[] parse = form.getParse();
        boolean sqlSplit = form.getSplit();
        
        String[] uFiles = new String[] {"users", "forum", "loads", "stat", "news", "comments"};

        Converter conv = new Converter(form.getPath(), form.getPref());
        conv.setLog(log);
        conv.setPassword(form.getPassword());
        conv.setWebAvatars(form.getWebAvatars());
        conv.setNoEmpty(!form.getEmpty());
        conv.setNoImage(!form.getImage());
        conv.setSmile(form.getSmile());
        conv.setNoFix(!form.getFix());
        conv.setVersion(form.getVersion());
        conv.setSiteName(form.getSiteNameOld(), form.getSiteNameNew(), form.getPostOnForum());

        log.println("Stage 1: loading backup files...");
        if (conv != null && conv.initUsers() && log.isVisible()) {
            log.setProgress(1, parse.length * 4 + 2);
            conv.loadBackups(parseAll, parse);
            if (!log.isVisible()) {
                log.println("ERROR: Dialog has been closed.");
                return;
            }
            log.println("Stage 2: copying files and updating links...");
            conv.linksUpdate();
            if (!log.isVisible()) {
                log.println("ERROR: Dialog has been closed.");
                return;
            }
            log.println("Stage 3: parsing backup...");
            ArrayList atmData = null;
            if (!sqlSplit) {
                atmData = new ArrayList();
            }
            ArrayList temp = conv.getSQL();
            if (!log.isVisible()) {
                log.println("ERROR: Dialog has been closed.");
                return;
            }
            if (temp != null) {
                for (int i = 0; i < temp.size(); i++) {
                    log.setProgress(log.getProgress() + 1);
                    if ((!parseAll && !parse[i]) || temp.get(i) == null) continue;
                    if (sqlSplit) {
                        String filename = "atomm_" + uFiles[i] + ".sql";
                        log.println( "Save \"" + filename + "\"..." );
                        createFile(filename, (ArrayList)temp.get(i));
                    } else {
                        atmData.addAll((ArrayList)temp.get(i));
                    }
                }
            }
            if (atmData != null) {
                log.println( "Save \"atomm.sql\"..." );
                createFile("atomm.sql", atmData);
            }
            log.setProgress(log.getProgress() + 1);
            log.println( "OK!" );
        }
    }
}
