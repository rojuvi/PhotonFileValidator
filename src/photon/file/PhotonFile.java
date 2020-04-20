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

package photon.file;

import photon.file.parts.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * by bn on 30/06/2018.
 */
public class PhotonFile {
    private PhotonFileHeader photonFileHeader;
    private PhotonFilePrintParameters photonFilePrintParameters;
    private PhotonFilePreview previewOne;
    private PhotonFilePreview previewTwo;
    private List<PhotonFileLayer> layers;

    private StringBuilder islandList;
    private int islandLayerCount;
    private ArrayList<Integer> islandLayers;
    private SortedSet<PhotonMultiLayerIsland> multiLayerIslands;

    private int margin;
    private ArrayList<Integer> marginLayers;

    public PhotonFile readFile(File file) throws Exception {
        return readFile(getBinaryData(file));
    }

    public PhotonFile readFile(File file, IPhotonProgress iPhotonProgress) throws Exception {
        return readFile(getBinaryData(file), iPhotonProgress);
    }

    public PhotonFile readFile(byte[] file) throws Exception {
        return readFile(file, new DummyPhotonLoadProgress());
    }

    public PhotonFile readFile(byte[] file, IPhotonProgress iPhotonProgress) throws Exception {
        iPhotonProgress.showInfo("Reading photon file header information...");
        photonFileHeader = new PhotonFileHeader(file);
        iPhotonProgress.showInfo("Reading photon large preview image information...");
        previewOne = new PhotonFilePreview(photonFileHeader.getPreviewOneOffsetAddress(), file);
        iPhotonProgress.showInfo("Reading photon small preview image information...");
        previewTwo = new PhotonFilePreview(photonFileHeader.getPreviewTwoOffsetAddress(), file);
        if (photonFileHeader.getVersion() > 1) {
            iPhotonProgress.showInfo("Reading Print parameters information...");
            photonFilePrintParameters = new PhotonFilePrintParameters(photonFileHeader.getPrintParametersOffsetAddress(), file);
        }
        iPhotonProgress.showInfo("Reading photon layers information...");
        layers = PhotonFileLayer.readLayers(photonFileHeader, file, margin, iPhotonProgress);
        resetMarginAndIslandInfo();

        return this;
    }

    public void saveFile(File file) throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        writeFile(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public byte[] saveFile() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeFile(baos);
        return baos.toByteArray();
    }

    private void writeFile(OutputStream outputStream) throws Exception {
        int antiAliasLevel = 1;
        if (photonFileHeader.getVersion() > 1) {
            antiAliasLevel = photonFileHeader.getAntiAliasingLevel();
        }

        int headerPos = 0;
        int previewOnePos = headerPos + photonFileHeader.getByteSize();
        int previewTwoPos = previewOnePos + previewOne.getByteSize();
        int layerDefinitionPos = previewTwoPos + previewTwo.getByteSize();

        int parametersPos = 0;
        if (photonFileHeader.getVersion() > 1) {
            parametersPos = layerDefinitionPos;
            layerDefinitionPos = parametersPos + photonFilePrintParameters.getByteSize();
        }

        int dataPosition = layerDefinitionPos + (PhotonFileLayer.getByteSize() * photonFileHeader.getNumberOfLayers() * antiAliasLevel);


        PhotonOutputStream os = new PhotonOutputStream(outputStream);

        photonFileHeader.save(os, previewOnePos, previewTwoPos, layerDefinitionPos, parametersPos);
        previewOne.save(os, previewOnePos);
        previewTwo.save(os, previewTwoPos);

        if (photonFileHeader.getVersion() > 1) {
            photonFilePrintParameters.save(os);
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            dataPosition = layer.savePos(dataPosition);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    dataPosition = layer.getAntiAlias(a).savePos(dataPosition);
                }
            }
        }

        // Order for backward compatibility with photon/cbddlp version 1
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            layers.get(i).save(os);
        }

        if (antiAliasLevel > 1) {
            for (int a = 0; a < (antiAliasLevel - 1); a++) {
                for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
                    layers.get(i).getAntiAlias(a).save(os);
                }
            }
        }

        // Optimize order for speed read on photon
        for (int i = 0; i < photonFileHeader.getNumberOfLayers(); i++) {
            PhotonFileLayer layer = layers.get(i);
            layer.saveData(os);
            if (antiAliasLevel > 1) {
                for (int a = 0; a < (antiAliasLevel - 1); a++) {
                    layer.getAntiAlias(a).saveData(os);
                }
            }
        }
    }


    private byte[] getBinaryData(File entry) throws Exception {
        if (entry.isFile()) {
            int fileSize = (int) entry.length();
            byte[] fileData = new byte[fileSize];

            InputStream stream = new FileInputStream(entry);
            int bytesRead = 0;
            while (bytesRead < fileSize) {
                int readCount = stream.read(fileData, bytesRead, fileSize - bytesRead);
                if (readCount < 0) {
                    throw new IOException("Could not read all bytes of the file");
                }
                bytesRead += readCount;
            }

            return fileData;
        }
        return null;
    }

    public String getInformation() {
        if (photonFileHeader == null) return "";
        return String.format("T: %.3f", photonFileHeader.getLayerHeight()) +
                ", E: " + formatSeconds(photonFileHeader.getNormalExposure()) +
                ", O: " + formatSeconds(photonFileHeader.getOffTime()) +
                ", BE: " + formatSeconds(photonFileHeader.getBottomExposureTimeSeconds()) +
                String.format(", BL: %d", photonFileHeader.getBottomLayers());
    }

    public String formatSeconds(float time) {
        if (time % 1 == 0) {
            return String.format("%.0fs", time);
        } else {
            return String.format("%.1fs", time);
        }
    }

    public int getIslandLayerCount() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayerCount;
    }

    public ArrayList<Integer> getIslandLayers() {
        if (islandList == null) {
            findIslands();
        }
        return islandLayers;
    }

    public SortedSet<PhotonMultiLayerIsland> getMultiLayerIslands() {
        if (islandList == null) {
            findIslands();
        }
        return multiLayerIslands;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    public int getMargin() {
        return this.margin;
    }

    public ArrayList<Integer> getMarginLayers() {
        if (marginLayers == null) {
            return new ArrayList<>();
        }
        return marginLayers;
    }

    public String getMarginInformation() {
        if (marginLayers == null) {
            return "No safety margin set, printing to the border.";
        } else {
            if (marginLayers.size() == 0) {
                return "The model is within the defined safety margin (" + this.margin + " pixels).";
            } else if (marginLayers.size() == 1) {
                return "The layer " + marginLayers.get(0) + " contains model parts that extend beyond the margin.";
            }
            StringBuilder marginList = new StringBuilder();
            int count = 0;
            for (int layer : marginLayers) {
                if (count > 10) {
                    marginList.append(", ...");
                    break;
                } else {
                    if (marginList.length() > 0) marginList.append(", ");
                    marginList.append(layer);
                }
                count++;
            }
            return "The layers " + marginList.toString() + " contains model parts that extend beyond the margin.";
        }
    }

    public String getLayerInformation() {
        if (islandList == null) {
            findIslands();
        }
        islandList = new StringBuilder();
        for (int i = 0; i < islandLayers.size() && i < 11; i++) {
            if (i == 10) {
                islandList.append(", ...");
            } else {
                if (islandList.length() > 0) islandList.append(", ");
                islandList.append(islandLayers.get(i));
            }
        }
        if (islandLayerCount == 0) {
            return "Whoopee, all is good, no unsupported areas";
        } else if (islandLayerCount == 1) {
            return "Unsupported islands found in layer " + islandList.toString();
        }
        return "Unsupported islands found in layers " + islandList.toString();
    }

    private void findIslands() {
        if (islandLayers != null) {
            islandLayers.clear();
            multiLayerIslands.clear();
            islandLayerCount = 0;
            SortedSet<PhotonMultiLayerIsland> prevLayerIslands = new TreeSet<>();
            if (layers != null) {
                for (int layerNo = 0; layerNo < photonFileHeader.getNumberOfLayers(); layerNo++) {
                    findIslands(layerNo, layers.get(layerNo));
                }
                reduce(multiLayerIslands);
            }
        }
    }

    public void findIslands(int layerNo, PhotonFileLayer layer) {
        if (layer.getIsLandsCount() > 0) {
            if (!islandLayers.contains(layerNo)) {
                islandLayerCount++;
                islandLayers.add(layerNo);
            }
        } else {
            if (islandLayers.contains(layerNo)) {
                islandLayerCount--;
                islandLayers.remove(Integer.valueOf(layerNo));
            }
        }
        findLayerIslands(layerNo, layer);
    }

    public void reduceMultiLayerIslands() {
        reduce(multiLayerIslands);
    }

    private void reduce(SortedSet<PhotonMultiLayerIsland> multiLayerIslands) {
        AtomicBoolean reducing = new AtomicBoolean(true);
        Set<PhotonMultiLayerIsland> toRemove = new HashSet<>();
        int count = 0;
        SortedSet<PhotonMultiLayerIsland> tmp = new TreeSet<>(multiLayerIslands);
        multiLayerIslands.clear();
        multiLayerIslands.addAll(tmp);
        while (reducing.get()) {
            tmp.clear();
            reducing.set(false);
            toRemove.clear();
            multiLayerIslands.forEach(mli -> {
                SortedSet<PhotonMultiLayerIsland> layerIslandsAhead = multiLayerIslands.tailSet(mli);
                for (PhotonMultiLayerIsland photonMultiLayerIsland : layerIslandsAhead) {
                    if (!mli.equals(photonMultiLayerIsland) && (mli.collidesWith(photonMultiLayerIsland) || mli.inContactWith(photonMultiLayerIsland))) {
                        reducing.set(true);
                        toRemove.remove(mli);
                        mli.merge(photonMultiLayerIsland);
                        if (!mli.equals(photonMultiLayerIsland)) {
                            toRemove.add(photonMultiLayerIsland);
                        }
                    }
                    if (mli.getEnd() < photonMultiLayerIsland.getStart()) {
                        break;
                    }
                }
                tmp.add(mli);
            });
            tmp.removeAll(toRemove);
            multiLayerIslands.clear();
            multiLayerIslands.addAll(tmp);
            count++;
        }
    }

    private void findLayerIslands(int layerNo, PhotonFileLayer layer) {
        if (layer.getPackedLayerImage() != null) {
            SortedSet<PhotonRect> layerIslands = layer.getIslandsRects();
            SortedSet<PhotonMultiLayerIsland> layerMultiLayerIslands = new TreeSet<>();
            boolean collisions;
            Set<PhotonMultiLayerIsland> possibleMergeableIslands = multiLayerIslands.stream()
                    .filter(island -> layerNo >= island.getStart() - 1 && layerNo <= island.getEnd() + 1)
                    .collect(Collectors.toSet());
            for (PhotonRect layerIsland : layerIslands) {
                collisions = false;
                for (PhotonMultiLayerIsland prevLayerIsland : possibleMergeableIslands) {
                    if (layerIsland.collidesWith(prevLayerIsland.getRect())
                            || layerIsland.inContactWith(prevLayerIsland.getRect())) {
                        collisions = true;
                        // Use prevLayerIsland as this layer island
                        layerMultiLayerIslands.add(prevLayerIsland);
                        prevLayerIsland.getRect().merge(layerIsland);
                        prevLayerIsland.setEnd(Math.max(layerNo, prevLayerIsland.getEnd()));
                        prevLayerIsland.setStart(Math.min(layerNo, prevLayerIsland.getStart()));
                        layerMultiLayerIslands.add(prevLayerIsland);
                    }
                }
                if (!collisions) {
                    layerMultiLayerIslands.add(new PhotonMultiLayerIsland(layerNo, layerIsland));
                }
            }

            SortedSet<PhotonMultiLayerIsland> toUpdate = new TreeSet<>();
            for (PhotonMultiLayerIsland prevLayerIsland : possibleMergeableIslands) {
                boolean keepLayer = false;
                for (PhotonMultiLayerIsland layerMultiLayerIsland : layerMultiLayerIslands) {
                    if (prevLayerIsland.getEnd() == layerNo - 1
                            || prevLayerIsland.getStart() == layerNo + 1
                            || prevLayerIsland.collidesWith(layerMultiLayerIsland)) {
                        keepLayer = true;
                    }
                }
                if (!keepLayer) {
                    toUpdate.add(prevLayerIsland);
                }
            }
            removeLayerIsland(toUpdate, layerNo);

            multiLayerIslands.addAll(layerMultiLayerIslands);
        }
    }

    private void removeLayerIsland(SortedSet<PhotonMultiLayerIsland> toUpdate, int layerNo) {
        for (PhotonMultiLayerIsland photonMultiLayerIsland : toUpdate) {
            removeLayerIsland(photonMultiLayerIsland, layerNo);
        }
    }

    private void removeLayerIsland(PhotonMultiLayerIsland toUpdate, int layerNo) {
        if (toUpdate.getStart() == layerNo && toUpdate.getEnd() == layerNo) {
            multiLayerIslands.remove(toUpdate);
        } else if (toUpdate.getStart() == layerNo) {
            toUpdate.setStart(layerNo + 1);
        } else if (toUpdate.getEnd() == layerNo) {
            toUpdate.setEnd(layerNo - 1);
        } else if (toUpdate.getStart() < layerNo && toUpdate.getEnd() > layerNo) {
            splitIsland(toUpdate, layerNo);
        }
    }

    private void splitIsland(PhotonMultiLayerIsland toUpdate, int layerNo) {
        PhotonMultiLayerIsland island1 = new PhotonMultiLayerIsland(toUpdate.getStart(), layerNo - 1, toUpdate.getRect());
        PhotonMultiLayerIsland island2 = new PhotonMultiLayerIsland(layerNo + 1, toUpdate.getEnd(), toUpdate.getRect());
        multiLayerIslands.remove(toUpdate);
        multiLayerIslands.add(island1);
        multiLayerIslands.add(island2);
    }


    public int getWidth() {
        return photonFileHeader.getResolutionY();
    }

    public int getHeight() {
        return photonFileHeader.getResolutionX();
    }

    public int getLayerCount() {
        return photonFileHeader.getNumberOfLayers();
    }

    public PhotonFileLayer getLayer(int i) {
        if (layers != null && layers.size() > i) {
            return layers.get(i);
        }
        return null;
    }

    public List<PhotonFileLayer> getLayers() {
        return layers;
    }

    public long getPixels() {
        long total = 0;
        if (layers != null) {
            for (PhotonFileLayer layer : layers) {
                total += layer.getPixels();
            }
        }
        return total;
    }

    public PhotonFileHeader getPhotonFileHeader() {
        return photonFileHeader;
    }

    public PhotonFilePreview getPreviewOne() {
        return previewOne;
    }

    public PhotonFilePreview getPreviewTwo() {
        return previewTwo;
    }


    public void unLink() {
        while (!layers.isEmpty()) {
            PhotonFileLayer layer = layers.remove(0);
            layer.unLink();
        }
        if (islandLayers != null) {
            islandLayers.clear();
        }
        if (multiLayerIslands != null) {
            multiLayerIslands.clear();
        }
        if (marginLayers != null) {
            marginLayers.clear();
        }
        photonFileHeader.unLink();
        photonFileHeader = null;
        previewOne.unLink();
        previewOne = null;
        previewTwo.unLink();
        previewTwo = null;
    }


    public void adjustLayerSettings() {
        for (int i = 0; i < layers.size(); i++) {
            PhotonFileLayer layer = layers.get(i);
            if (i < photonFileHeader.getBottomLayers()) {
                layer.setLayerExposure(photonFileHeader.getBottomExposureTimeSeconds());
            } else {
                layer.setLayerExposure(photonFileHeader.getNormalExposure());
            }
            layer.setLayerOffTimeSeconds(photonFileHeader.getOffTimeSeconds());
        }
    }

    public void fixLayers(IPhotonProgress progres) throws Exception {
        PhotonLayer layer = null;
        int i = 0;
        ArrayList<Integer> islandLayers = new ArrayList<>(this.islandLayers);
        for (int layerNo : islandLayers) {
            int untilNextLayer = -1;
            if (i+1 < islandLayers.size()) {
                untilNextLayer = islandLayers.get(i+1) - layerNo;
            }
            progres.showInfo("Checking layer " + layerNo);

            // Unpack the layer data to the layer utility class
            PhotonFileLayer fileLayer = layers.get(layerNo);
            if (layer == null) {
                layer = fileLayer.getLayer();
            } else {
                fileLayer.getUpdateLayer(layer);
            }
            int changed = fixit(progres, layer, fileLayer, 10);
            if (changed == 0) {
                progres.showInfo(", but nothing could be done.");
            } else {
                fileLayer.saveLayer(layer);
                calculate(layerNo, untilNextLayer);
            }

            progres.showInfo("<br>");
            i++;
        }
        findIslands();
    }

    private int fixit(IPhotonProgress progres, PhotonLayer layer, PhotonFileLayer fileLayer, int loops) throws Exception {
        int changed = layer.fixlayer();
        if (changed > 0) {
            layer.reduce();
            fileLayer.updateLayerIslands(layer);
            progres.showInfo(", " + changed + " pixels changed");
            if (loops > 0) {
                changed += fixit(progres, layer, fileLayer, loops - 1);
            }
        }
        return changed;
    }

    public void calculateAaLayers(IPhotonProgress progres, PhotonAaMatrix photonAaMatrix) throws Exception {
        PhotonFileLayer.calculateAALayers(photonFileHeader, layers, photonAaMatrix, progres);
    }

    public void calculate(IPhotonProgress progres) throws Exception {
        resetMarginAndIslandInfo();
        PhotonFileLayer.calculateLayers(this, progres);
    }

    public void calculate(int layerNo, int limit) throws Exception {
        PhotonFileLayer.calculateLayers(this, layerNo, limit);
//        resetMarginAndIslandInfo();
    }

    public void restartIslandInfo() {
        getIslandLayers().clear();
        getMultiLayerIslands().clear();
        islandList = new StringBuilder();
        islandLayerCount = 0;
    }

    private void resetMarginAndIslandInfo() {
        islandList = null;
        islandLayerCount = 0;
        islandLayers = new ArrayList<>();
        multiLayerIslands = new TreeSet<>();

        if (margin > 0) {
            marginLayers = new ArrayList<>();
            int i = 0;
            for (PhotonFileLayer layer : layers) {
                if (layer.doExtendMargin()) {
                    marginLayers.add(i);
                }
                i++;
            }
        }
    }

    public float getZdrift() {
        float expectedHeight = photonFileHeader.getLayerHeight() * (photonFileHeader.getNumberOfLayers() - 1);
        float actualHeight = layers.get(layers.size() - 1).getLayerPositionZ();
        return expectedHeight - actualHeight;

    }

    public void fixLayerHeights() {
        int index = 0;
        for (PhotonFileLayer layer : layers) {
            layer.setLayerPositionZ(index * photonFileHeader.getLayerHeight());
            index++;
        }
    }

    public boolean hasAA() {
        return (photonFileHeader.getVersion()>1 && photonFileHeader.getAntiAliasingLevel()>1);
    }

    public int getAALevels() {

        if (photonFileHeader.getVersion()>1) {
            return photonFileHeader.getAntiAliasingLevel();
        }
        return 1;
    }

    public PhotonFilePrintParameters getPhotonFileParameters() {
        return photonFilePrintParameters;
    }

    public void changeToVersion2() {
        photonFileHeader.makeVersion(2);
        photonFilePrintParameters = new PhotonFilePrintParameters(photonFileHeader.getBottomLayers());
    }

    // only call this when recalculating AA levels
    public void setAALevels(int levels) {
        if (photonFileHeader.getVersion()>1) {
            if (levels < photonFileHeader.getAntiAliasingLevel()) {
                reduceAaLevels(levels);
            }
            if (levels > photonFileHeader.getAntiAliasingLevel()) {
                increaseAaLevels(levels);
            }
        }
    }

    private void increaseAaLevels(int levels) {
        // insert base layer to the correct count, as we are to recalc the AA anyway
        for(PhotonFileLayer photonFileLayer : layers) {
            while (photonFileLayer.getAntiAlias().size()<(levels-1)) {
                photonFileLayer.getAntiAlias().add(new PhotonFileLayer(photonFileLayer, photonFileHeader));
            }
        }
        photonFileHeader.setAntiAliasingLevel(levels);
    }

    private void reduceAaLevels(int levels) {
        // delete any layers to the correct count, as we are to recalc the AA anyway
        for (PhotonFileLayer photonFileLayer : layers) {
            while (photonFileLayer.getAntiAlias().size() > (levels - 1)) {
                photonFileLayer.getAntiAlias().remove(0);
            }
        }
        photonFileHeader.setAntiAliasingLevel(levels);
    }


    public void removeIslands(SortedSet<PhotonMultiLayerIsland> islandsToRemove, Set<Integer> layers, IPhotonProgress progress) throws Exception {
        int count = 1;
        for (int layerNo : layers) {
            SortedSet<PhotonMultiLayerIsland> inThisLayer = islandsToRemove.stream()
                .filter(island -> island.getStart() <= layerNo && island.getEnd() >= layerNo)
                .collect(Collectors.toCollection(TreeSet::new));
            removeIslands(inThisLayer, layerNo);
            progress.showInfo(Integer.toString(count));
            count++;
        }
    }

    private void removeIslands(Set<PhotonMultiLayerIsland> islandsToRemove, int layerNo) throws Exception {
        this.getLayer(layerNo).removeIslands(islandsToRemove);
        calculate(layerNo, 1);
    }

}

