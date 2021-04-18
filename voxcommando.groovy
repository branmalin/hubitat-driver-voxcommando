metadata {
    definition (
        name: "VoxCommando Speech Synthesizer",
        namespace: "BranMalin",
        author: "Bran Malin",
        description: "A simple Hubitat driver for sending speech synthesis commands & others to the VoxCommando voice assistant."
    )
    {
        capability "SpeechSynthesis"
        capability "SpeechRecognition"

        command "listenOn"
        command "listenOff"
        command "listenStandby"
        command "sendCommand", [
            [name: "VoxCommand*", type: "STRING", description: "Command to send"],
            [name: "Param1", type: "STRING", description: "Optional parameter"],
            [name: "Param2", type: "STRING", description: "Optional parameter"],
            [name: "Param3", type: "STRING", description: "Optional parameter"],
            [name: "Param4", type: "STRING", description: "Optional parameter"]]
        attribute "listenMode", "ENUM", ["On", "Off", "Standby"]
    }
    preferences {
        input name: "ipAddress", type: "text", title: "IP Address", description: "Vox Commando Installation IP Address", required: true
        input name: "port", type: "number", range: "1..65532", title: "Vox Commando Port", defaultValue: 33000, description: "Port used by Vox Commando UDP Port Listener", required: true
        input name: "speechCommand", type: "enum", options: ["TTS.Speak", "TTS.SpeakSync", "VC.TriggerEvent"], title: "Speech Command Type", description: "Command sent to Vox Commando to generate speech.", defaultValue: "TTS.SpeakSync", required: true
        input name: "speechEvent", type: "text", title: "Speech Event", description: "Event name to use if speech is executed via triggered event.", defaultValue: "speak", required: false
    }
}

// Utility functions.

// Convert a human-readable IP string to a hexadecimal string representation. 
// Device Network Identifiers have to be reported to Hubitat in a hexadecimal representation.
private String convertIPtoHex(String ipAddressHumanReadable) 
{ 
    String ipAddressHex = "";
    ipAddressHumanReadable.split("\\.").each({ipAddressHex += String.format("%02x", it.toInteger())});
    return ipAddressHex.toUpperCase();
}

//Attributes: phraseSpoken - STRING
//Commands: speak(text, volume, voice)

void updated()
{
    log.info "Updating Device Parameters";    
    //The Hub needs to know the address from which to expect messages from VoxCommando.
    //It requires the address in the form of a string containing a hexadecimal representation.
    String ipAddressHex = convertIPtoHex(ipAddress);
    //Inform the hub of the address of the VoxCommando node.
    log.info "Setting Device Network Address to ${ipAddressHex}";
    device.setDeviceNetworkId(ipAddressHex);
}

void parse(String message)
{
    body = parseLanMessage(message).body;
    log.info("Received message ${body}.");
    events = body.split("&&");
    log.info("Parsed to events ${events}");
    events.each({
        parts = it.split("=",2);
        log.info("Parsed ${it} to parts ${parts}");
        eventType = parts[0].toLowerCase();
        eventData = parts[1];
        switch(eventType)
        {
            case "status":
                sendEvent(name: "listenMode", value: eventData);
                break;
            case "speech":
                sendEvent(name: "phraseSpoken", value: eventData);
                break;
            default:
                log.warn("Unrecognized event from VoxCommando! ${eventType}: ${eventData}");
                break;
        }
    })
}

void sendCommand(String VoxCommand, String Param1 = "", String Param2 = "", String Param3 = "", String Param4 = "")
{
    //Add parameters to the command string
    [Param1, Param2, Param3, Param4].each({VoxCommand += (it.equals("") ? "" : "&&${it}")});
    log.info("Built Command String: ${VoxCommand}");

    //Build and send a UDP message for Vox Commando
    def action = new hubitat.device.HubAction(VoxCommand, hubitat.device.Protocol.LAN, [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT, destinationAddress: "${ipAddress}:${port}", ignoreResponse: true]);
    sendHubCommand(action);
    log.info("Sent Command.");
}

void speak(String text, Integer volume = null, String voice = null)
{
    if (volume != null)
    {
        if (volume <= 100 && volume >= 0)
        {
            sendCommand("Sound.SetVol", volume);
        }
        else
        {
            log.warn("Command used with out-of-range volume! ${volume}");
        }
    }
    if (speechCommand.equals("VC.TriggerEvent"))
    {
        sendCommand("VC.TriggerEvent", speechEvent, text);
    }
    else
    {
        sendCommand(speechCommand, text);
    }
}

void listenOn()
{
    sendCommand("VC.On");
}

void listenStandby()
{
    sendCommand("VC.Standby");
}

void listenOff()
{
    sendCommand("VC.Off");
}