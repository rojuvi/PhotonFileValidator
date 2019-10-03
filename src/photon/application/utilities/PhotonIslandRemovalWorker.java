/*
 * MIT License
 *
 * Copyright (c) 2018 Bonosoft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package photon.application.utilities;

import photon.application.MainForm;
import photon.application.dialogs.RemoveIslandsDialog;
import photon.file.PhotonFile;
import photon.file.parts.IPhotonProgress;
import photon.file.parts.PhotonMultiLayerIsland;

import javax.swing.*;
import java.util.SortedSet;

/**
 * by bn on 14/07/2018.
 */
public class PhotonIslandRemovalWorker extends SwingWorker<Integer, String> implements IPhotonProgress {
    private MainForm mainForm;
    private RemoveIslandsDialog removeIslandsDialog;
    private PhotonFile photonFile;
    private SortedSet<PhotonMultiLayerIsland> islandsToRemove;

    public PhotonIslandRemovalWorker(MainForm mainForm, RemoveIslandsDialog removeIslandsDialog, PhotonFile photonFile) {
        this.mainForm = mainForm;
        this.removeIslandsDialog = removeIslandsDialog;
        this.photonFile = photonFile;
        islandsToRemove = null;
    }

    public PhotonIslandRemovalWorker(MainForm mainForm, RemoveIslandsDialog removeIslandsDialog, PhotonFile photonFile, SortedSet<PhotonMultiLayerIsland> islandsToRemove) {
        this.mainForm = mainForm;
        this.removeIslandsDialog = removeIslandsDialog;
        this.photonFile = photonFile;
        this.islandsToRemove = islandsToRemove;
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        if (!chunks.isEmpty()) {
            removeIslandsDialog.setProgress(Integer.valueOf(chunks.get(chunks.size() - 1)));
        }
    }

    @Override
    protected void done() {
        removeIslandsDialog.setInformation(photonFile);
        removeIslandsDialog.activate();
        mainForm.showMarginAndIslandInformation();
        mainForm.changeLayer();
    }

    @Override
    public void showInfo(String str) {
        publish(str);
    }

    @Override
    protected Integer doInBackground() {
        removeIslandsDialog.lockdown();
        if (islandsToRemove == null) {
            islandsToRemove = photonFile.getMultiLayerIslands();
        }
        int start = islandsToRemove.first().getStart();
        int end = islandsToRemove.last().getEnd();
        removeIslandsDialog.setMinProgress(start - 1);
        removeIslandsDialog.setMaxProgress(end);
        removeIslandsDialog.setProgress(start - 1);
        try {
            mainForm.photonFile.removeIslands(islandsToRemove, this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return 1;
    }
}
