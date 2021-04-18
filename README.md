# hubitat-driver-voxcommando
Simple Hubitat Driver for Vox Commando

This driver allows Hubitat to interact with an installation of the Vox Commando voice assistant as a device with Speech Synthesis and Speech Recognition capabilities.
For full function, Vox Commando needs to be configured to transmit recognized speech and listening state (always listen, wakeword, or off) to Hubitat.
The XML files incorporated here contain Vox Commando commands and events to do this, along with a map that should be filled in with the IP address of your hubitat and the port on which it listens (standard port is prefilled).