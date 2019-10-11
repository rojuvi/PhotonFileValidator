package photon.application.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import photon.application.MainForm;
import photon.application.utilities.PhotonIslandRemovalWorker;
import photon.file.PhotonFile;
import photon.file.parts.PhotonMultiLayerIsland;
import photon.file.ui.PhotonLayerImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class RemoveIslandsDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOk;

    private JButton removeSelectedButton;
    private JScrollPane islandListScroll;
    private JCheckBox checkBox1;
    private JPanel islandList;
    private JScrollPane imageScroll;
    private JSlider layerSlider;
    private JLabel layerCount;
    private JButton removeFiltered;
    private JProgressBar progressBar;
    private JSpinner layerNumberSpinner;
    private JSpinner boxSizeSpinner;
    private PhotonLayerImage image;
    private RemoveIslandsDialog that;

    private boolean lockdown = false;

    private JLabel currentSelected = null;

    private PhotonFile photonFile;
    private Set<PhotonMultiLayerIsland> multiLayerIslands;
    private SortedSet<PhotonMultiLayerIsland> selectedIslands = new TreeSet<>();
    private SortedSet<PhotonMultiLayerIsland> filteredIslands = new TreeSet<>();
    private ArrayList<JCheckBox> islandCheckboxes = new ArrayList<>();
    PhotonMultiLayerIsland selectedIsland = null;
    private List<JCheckBox> checkboxes;

    public RemoveIslandsDialog(final MainForm mainForm) {
        super(mainForm.frame);
        that = this;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOk);

        buttonOk.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        image = new PhotonLayerImage(2560, 1440);
        imageScroll.setPreferredSize(new Dimension(2560, 1440));
        imageScroll.setViewportView(image);

        layerSlider.addChangeListener(e -> {
            layerCount.setText(Integer.toString(layerSlider.getValue()));
            layerCount.repaint();
            image.drawLayer(photonFile.getLayer(layerSlider.getValue()), photonFile.getMargin());
            image.drawRect(selectedIsland.getRect(), 5);
            image.repaint();
        });

        removeSelectedButton.addActionListener(e -> {
            PhotonIslandRemovalWorker photonIslandRemovalWorker = new PhotonIslandRemovalWorker(mainForm, that, photonFile, selectedIslands);
            photonIslandRemovalWorker.execute();
        });

        removeFiltered.addActionListener(e -> {
            PhotonIslandRemovalWorker photonIslandRemovalWorker = new PhotonIslandRemovalWorker(mainForm, that, photonFile, filteredIslands);
            photonIslandRemovalWorker.execute();
        });

        layerNumberSpinner.addChangeListener(e -> {
            filterIslands();
            addIslandsToList();
            activate();
        });

        boxSizeSpinner.addChangeListener(e -> {
            filterIslands();
            addIslandsToList();
            activate();
        });

        checkboxes = new ArrayList<>();
    }

    private void filterIslands() {
        int layerFilter = (int) layerNumberSpinner.getValue();
        int sizeFilter = (int) boxSizeSpinner.getValue();
        filteredIslands = multiLayerIslands.parallelStream()
                .filter(island -> (layerFilter == 0 || island.getEnd() - island.getStart() < layerFilter)
                        && (sizeFilter == 0 || island.getRect().getArea() <= sizeFilter))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void onOK() {
        if (!lockdown) {
            dispose();
        }
    }

    private void onCancel() {
        if (!lockdown) {
            dispose();
        }
    }

    public void setInformation(PhotonFile photonFile) {
        this.photonFile = photonFile;
        this.multiLayerIslands = photonFile.getMultiLayerIslands();
        filterIslands();
        addIslandsToList();
        activate();
    }

    private void addIslandsToList() {
        islandList.removeAll();
        checkboxes.clear();
        int i = 0;
        islandList.setLayout(new GridLayoutManager(this.filteredIslands.size() + 1, 2, new Insets(0, 0, 0, 0), -1, -1));
        for (PhotonMultiLayerIsland multiLayerIsland : this.filteredIslands) {
            addIslandToList(multiLayerIsland, i);
            i++;
        }

        islandList.add(new Spacer(), new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        islandList.revalidate();
    }


    private void addIslandToList(PhotonMultiLayerIsland multiLayerIsland, int pos) {
        JCheckBox checkbox = new JCheckBox();
        String label = pos + " -  from " + multiLayerIsland.getStart() + " to " + multiLayerIsland.getEnd();
        if (multiLayerIsland.getStart() == multiLayerIsland.getEnd()) {
            label += " (1 Layer)";
        }
        JLabel jLabel = new JLabel(label);
        jLabel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                image.drawLayer(photonFile.getLayer(multiLayerIsland.getStart()), photonFile.getMargin());
                selectedIsland = multiLayerIsland;
                int posX = multiLayerIsland.getRect().getX1() + (multiLayerIsland.getRect().getX2() - multiLayerIsland.getRect().getX1()) / 2 - (imageScroll.getWidth() / 2);
                if (posX < 0) {
                    posX = 0;
                }
                int posY = multiLayerIsland.getRect().getY1() + (multiLayerIsland.getRect().getY2() - multiLayerIsland.getRect().getY1()) / 2 - (imageScroll.getHeight() / 2);
                if (posY < 0) {
                    posY = 0;
                }
                imageScroll.getHorizontalScrollBar().setValue(posX);
                imageScroll.getVerticalScrollBar().setValue(posY);
                imageScroll.getHorizontalScrollBar().repaint();
                imageScroll.getVerticalScrollBar().repaint();
                imageScroll.repaint();
                if (currentSelected != null) {
                    currentSelected.setFont(currentSelected.getFont().deriveFont(Font.PLAIN));
                    currentSelected.repaint();
                }
                jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
                jLabel.repaint();
                currentSelected = jLabel;

                layerSlider.setEnabled(true);
                layerSlider.setMaximum(multiLayerIsland.getEnd() + 5);
                layerSlider.setMinimum(multiLayerIsland.getStart());
                layerSlider.setValue(multiLayerIsland.getStart());
                layerSlider.repaint();

                image.drawRect(multiLayerIsland.getRect(), 5);
                image.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        if (selectedIslands.contains(multiLayerIsland)) {
            checkbox.setSelected(true);
        }

        checkbox.addChangeListener(e -> {
            if (checkbox.isSelected()) {
                selectedIslands.add(multiLayerIsland);
            } else {
                selectedIslands.remove(multiLayerIsland);
            }
            activate();
        });

        islandList.add(checkbox, new GridConstraints(pos, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        islandList.add(jLabel, new GridConstraints(pos, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        if (!filteredIslands.contains(multiLayerIsland)) {
            checkbox.setVisible(false);
            jLabel.setVisible(false);
        }
    }


    public void setProgress(int value) {
        progressBar.setValue(value);
    }

    public void setMaxProgress(int value) {
        progressBar.setMaximum(value);
    }

    public void setMinProgress(int value) {
        progressBar.setMinimum(value);
    }

    public void activate() {
        lockdown = false;
        buttonOk.setEnabled(true);
        if (anySelected()) {
            removeSelectedButton.setEnabled(true);
        } else {
            removeSelectedButton.setEnabled(false);
        }
        if (anyFilteredIslands()) {
            removeFiltered.setEnabled(true);
        } else {
            removeFiltered.setEnabled(false);
        }
        islandList.setEnabled(true);
        contentPane.repaint();
    }

    public void lockdown() {
        lockdown = true;
        buttonOk.setEnabled(false);
        removeSelectedButton.setEnabled(false);
        removeFiltered.setEnabled(false);
        islandList.setEnabled(false);
        contentPane.repaint();
    }

    private boolean anySelected() {
        return !selectedIslands.isEmpty();
    }

    private boolean anyFilteredIslands() {
        return !filteredIslands.isEmpty();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMinimumSize(new Dimension(-1, -1));
        contentPane.setPreferredSize(new Dimension(-1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOk = new JButton();
        buttonOk.setText("Done");
        panel2.add(buttonOk, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, new Dimension(2147483647, 2147483647), 0, false));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setContinuousLayout(false);
        splitPane1.setLastDividerLocation(-1);
        splitPane1.setOrientation(1);
        panel3.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel4);
        imageScroll = new JScrollPane();
        panel4.add(imageScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        layerSlider = new JSlider();
        layerSlider.setEnabled(false);
        layerSlider.setOrientation(1);
        layerSlider.setPaintLabels(false);
        layerSlider.setPaintTicks(false);
        layerSlider.setPaintTrack(true);
        layerSlider.setSnapToTicks(true);
        layerSlider.setValue(0);
        panel4.add(layerSlider, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        layerCount = new JLabel();
        layerCount.setText("0");
        panel4.add(layerCount, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel5);
        islandListScroll = new JScrollPane();
        islandListScroll.setMinimumSize(new Dimension(300, -1));
        islandListScroll.setPreferredSize(new Dimension(300, -1));
        panel5.add(islandListScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        islandList = new JPanel();
        islandList.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        islandListScroll.setViewportView(islandList);
        checkBox1 = new JCheckBox();
        checkBox1.setText("CheckBox");
        islandList.add(checkBox1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        islandList.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        islandList.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        removeSelectedButton = new JButton();
        removeSelectedButton.setEnabled(false);
        removeSelectedButton.setText("Remove selected");
        panel8.add(removeSelectedButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel8.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Click on the text to show island");
        panel8.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar = new JProgressBar();
        progressBar.setMinimum(-1);
        progressBar.setStringPainted(false);
        progressBar.setValue(-1);
        panel7.add(progressBar, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 8, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        layerNumberSpinner = new JSpinner();
        panel9.add(layerNumberSpinner, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel9.add(spacer5, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        boxSizeSpinner = new JSpinner();
        panel9.add(boxSizeSpinner, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Max layers (0 for all)");
        panel9.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setInheritsPopupMenu(false);
        label3.setText("Filters:");
        panel9.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Max box size (px^2)(0 for any)");
        panel9.add(label4, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeFiltered = new JButton();
        removeFiltered.setActionCommand("");
        removeFiltered.setEnabled(false);
        removeFiltered.setLabel("Remove Filtered");
        removeFiltered.setText("Remove Filtered");
        panel9.add(removeFiltered, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel9.add(spacer6, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
