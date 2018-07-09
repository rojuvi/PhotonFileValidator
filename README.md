# Photon File Viewer
A file viewer application for AnyCubic Photon sliced files (*.photon and *.cbddlp). The viewer can show you preview images, print information and all layers with information on overhang and islands issues.

![Main screen](https://github.com/Photonsters/PhotonFileViewer/raw/master/doc/screen1.png)

## Photon files

### TL;DR
Basic information, with this application you can:
- View all layers of a sliced AnyCubic Photon Slicer for the Photon 3D printer.
- Easy see overhangs and unsupported areas (color coded in yellow and red)
- Change exposure, off time and buttom layer settings on already sliced files.
- Easy check if any part of a model is to close to the border
- Quickly jump to problem layers and watch if it will break your prints.

### What is a Photon file
A Photon file is generated by the AnyCubic Photon Slicer (rebrand of ChiTu DLP Slicer). The slicer takes a 3D object and turns it into a list of images, each representing
one printable layer. The 3D printer then prints each layer by exposing liquid resin with UV light, and lifting the build plate.

The Photon file also contains information about the height of each layer, for how long each layer is exposed to UV light, and some settings for overexposing the first layers to make the print stick to the build plate.

### Why do I want to view a photon file
In the ideal world, - you don't. The slicer makes the file, and the printer use the file to print your object.

But, sometimes prints fail.

There are a lot of reasons why prints are failing.

- Missing or weak support. When the printer is done printing one layer, the layer is lifted off the botom plastic film (FEP). The support must be strong enough to support the lift (pull).
- Wrong exposure or off time settings. Each Resin is different in the chemical content, so each resin have a limited time range where it cures each layer.
- Separated Resin. Most resin needs to be shaken or stirred befor use, which ensures that the resin compunds are mixed in the correct manor.
- File, model or slicing errors. Applications have bugs, so errors could be intruduced in the process.
- Printer errors. When buying a budget printer, some will only pass the quality control on a good day. Some components might be designed with no fault margin, introducing periodic errors (like the power supply and the LCD).

The Photon File Viewer can show you all the layers in full resolution and all settings stored in the file.

The Viewer can also analyze the file for layers that contains areas that is not supported (printed in mid-air, called islands). Islands are a problem, because the model will not be printed as designed.
In the best scenario the island will stick to the plastic bottom of the printer or attach itself to the printed model, but it could also be trapped between the printer and the model, breaking the FEP or the LCD screen.



## Installation
If you already have Java installed, you can simply download the jar file and execute it (dobbeltclick or from command line: java -jar PhotonFileCheck.jar

[Download Jar version](https://github.com/Photonsters/PhotonFileViewer/raw/master/release/photonfileviewer-1.0.zip)


### Install on Windows
![Windows Main screen](https://github.com/Photonsters/PhotonFileViewer/raw/master/doc/windows.png)

The windows installer is not signed, so you have to go through some warning pages before you are allowed to install it.

[Download Windows version](https://github.com/Photonsters/PhotonFileViewer/raw/master/release/photonfileviewer-1.0-windows-installer.exe)


### Install on macOS
Download the zip and unpack it, then run the installer. The installer is not signed, so you may have to locate the installer in finder and right click on it and select open.

[Download Mac version](https://github.com/Photonsters/PhotonFileViewer/raw/master/release/photonfileviewer-1.0-osx-installer.zip)

### Install on Linux
The linux installer is build from the same source, but not tested. Feel free to test and provide feedback.

[Download Linux version](https://github.com/Photonsters/PhotonFileViewer/raw/master/release/release/photonfileviewer-1.0-linux-installer.run)

## Developer Information

### Source code layout

### Code Implementation
