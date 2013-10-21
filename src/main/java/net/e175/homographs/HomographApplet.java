/**
 * A simple applet for finding homographs/homoglyphs:
 * different characters with identical glyphs, e.g. Latin small a (U+0061)
 * and Cyrillic small a (U+430).
 *
 * @author Klaus A. Brunner 2005-02-10 (updated 2008-05-11)
 */
package net.e175.homographs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class HomographApplet extends javax.swing.JApplet {

    /*
     * GUI code was generated using CloudGarden's Jigloo SWT/Swing GUI Builder,
     * which is free for non-commercial use.
     */
    private static final long serialVersionUID = 1L;

    static {
        // Set Look & Feel
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JComboBox fontCombo, sizeCombo;
    private JTextArea textArea;
    private JButton runButton;
    private GlyphComparatorThread iterator;

    /**
     * Auto-generated main method to display this JApplet inside a new JFrame.
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();
        HomographApplet inst = new HomographApplet();
        frame.getContentPane().add(inst);
        frame.getContentPane().setPreferredSize(inst.getSize());
        frame.pack();
        frame.setVisible(true);
        frame.setTitle("Homograph finder");
    }

    public HomographApplet() throws Exception {
        super();
        this.iterator = new GlyphComparatorThread(this);
        initGUI();
    }

    private void initGUI() {
        try {
            this.getContentPane().setLayout(null);
            this.setSize(400, 400);
            {
                GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                String envfonts[] = gEnv.getAvailableFontFamilyNames();
                Vector<String> vector = new Vector<String>();

                for (int i = 1; i < envfonts.length; i++) {
                    vector.addElement(envfonts[i]);
                }
                ComboBoxModel fontComboModel = new DefaultComboBoxModel(vector);
                fontCombo = new JComboBox();
                this.getContentPane().add(this.fontCombo);
                fontCombo.setModel(fontComboModel);
                fontCombo.setBounds(19, 12, 173, 29);
            }
            {

                String[] sizes = {"8", "10", "12", "14", "16"};
                sizeCombo = new JComboBox(sizes);
                this.getContentPane().add(this.sizeCombo);
                sizeCombo.setBounds(19, 52, 173, 30);
            }

            {
                runButton = new JButton();
                this.getContentPane().add(runButton);
                runButton.setText("Start");
                runButton.setBounds(204, 52, 165, 30);
                runButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent evt) {

                        if (!iterator.isRunning()) {
                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                            Font f = getSelectedFont();
                            iterator.setFont(f);
                            textArea.setFont(f);

                            iterator.setRunning(true);
                            fontCombo.setEnabled(false);
                            sizeCombo.setEnabled(false);
                            runButton.setText("Stop");
                            textArea.setText("Homographs for font " + f.getFontName() + " " + f.getSize() + ":\n");
                            iterator.start();
                        } else {
                            iterator.setRunning(false);
                            runButton.setEnabled(false);
                        }
                    }
                });
            }
            {
                textArea = new JTextArea();

                JScrollPane areaScrollPane = new JScrollPane(textArea);
                areaScrollPane.setBounds(19, 96, 350, 267);
                this.getContentPane().add(areaScrollPane);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Font getSelectedFont() {
        String fontchoice = (String) fontCombo.getSelectedItem();
        int fontSize = Integer.parseInt((String) sizeCombo.getSelectedItem());
        return new Font(fontchoice, Font.PLAIN, fontSize);
    }

    public void addMessage(String msg) {
        this.textArea.append(msg);
    }

    // callback function
    public void iteratorFinished() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        iterator = new GlyphComparatorThread(this);
        fontCombo.setEnabled(true);
        sizeCombo.setEnabled(true);
        runButton.setText("Start");
        runButton.setEnabled(true);
    }

}

class GlyphComparatorThread extends Thread {
    private final HomographApplet caller;
    private final GlyphComparator comparator;

    private static final char sourcelimit = '\u00FF'; // latin 1
    private static final char targetlimit = '\uFFFF'; // BMP

    public GlyphComparatorThread(HomographApplet caller) {
        this.comparator = new GlyphComparator(targetlimit);
        this.setDaemon(true);
        this.caller = caller;
    }

    public void run() {
        List<char[]> matches = this.comparator.compareGlyphs((char) Character.MIN_CODE_POINT, sourcelimit);
        this.comparator.run = false;

        for (char[] cs : matches) {
            String msg = (new Formatter()).format("U+%04X matches U+%04X : %c%c ", (int) cs[0],
                    (int) cs[1], cs[0], cs[1]).toString();

            caller.addMessage(msg + "\n");
        }

        caller.iteratorFinished();
    }

    public void setFont(Font font) {
        comparator.setFont(font);
    }

    public boolean isRunning() {
        return this.comparator.run;
    }

    public void setRunning(boolean running) {
        this.comparator.run = running;
    }
}
