package photon.application.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import photon.application.MainForm;
import photon.file.PhotonFile;
import photon.file.parts.PhotonMultiLayerIsland;
import photon.file.ui.PhotonLayerImage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Set;

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
    private PhotonLayerImage image;

    private JLabel currentSelected = null;

    private PhotonFile photonFile;
    private Set<PhotonMultiLayerIsland> multiLayerIslands;
    private ArrayList<JCheckBox> islandCheckboxes = new ArrayList<>();
    PhotonMultiLayerIsland selectedIsland = null;

    public RemoveIslandsDialog(final MainForm mainForm) {
        super(mainForm.frame);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOk);

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        islandListScroll.setMinimumSize(new Dimension(200, -1));
        image = new PhotonLayerImage(2560, 1440);
        imageScroll.setPreferredSize(new Dimension(2560, 1440));
        imageScroll.setViewportView(image);

        layerSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                layerCount.setText(Integer.toString(layerSlider.getValue()));
                layerCount.repaint();
                image.drawLayer(photonFile.getLayer(layerSlider.getValue()), photonFile.getMargin());
                image.drawRect(selectedIsland.getRect(), 5);
                image.repaint();
            }
        });
        contentPane.setSize(new Dimension(1280, 1000));
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public void setInformation(PhotonFile photonFile) {
        this.photonFile = photonFile;
        this.multiLayerIslands = photonFile.getMultiLayerIslands();
        addIslandsToList();
    }

    private void addIslandsToList() {
        islandList.removeAll();
        int i = 0;
        islandList.setLayout(new GridLayoutManager(this.multiLayerIslands.size() + 1, 2, new Insets(0, 0, 0, 0), -1, -1));
        for (PhotonMultiLayerIsland multiLayerIsland : this.multiLayerIslands) {
            JCheckBox checkbox = new JCheckBox();
            JLabel jLabel = new JLabel(i + " -  from " + multiLayerIsland.getStart() + " to " + multiLayerIsland.getEnd());
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

            islandList.add(checkbox, new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
            islandList.add(jLabel, new GridConstraints(i, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
            i++;
        }

        islandList.add(new Spacer(), new GridConstraints(i, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
    }
    // TODO make island supported stop when it connects to something supported
    // TODO implement preview of island when clicked
    // TODO implement select islands to remove
    // TODO implement remove islands
    // TODO implement remove all islands


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
        contentPane.setPreferredSize(new Dimension(1280, 1000));
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
        contentPane.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setContinuousLayout(false);
        splitPane1.setLastDividerLocation(199);
        splitPane1.setOrientation(1);
        panel3.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        islandListScroll = new JScrollPane();
        splitPane1.setLeftComponent(islandListScroll);
        islandList = new JPanel();
        islandList.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        islandListScroll.setViewportView(islandList);
        checkBox1 = new JCheckBox();
        checkBox1.setText("CheckBox");
        islandList.add(checkBox1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        islandList.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        islandList.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        contentPane.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        removeSelectedButton = new JButton();
        removeSelectedButton.setEnabled(false);
        removeSelectedButton.setText("Remove selected");
        panel7.add(removeSelectedButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Click on the text to show island");
        panel7.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
