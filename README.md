# SmartEnvironmentProject

This is an old Undergrad Final Year Project (TCC - Trabalho de Conclusão de Curso), which, to the writer's late surprise, is surprisingly poor on comments and data logging. 

The project automates an undergraduate student's bedroom light and bed lamp by sensing the environment and actuating according to the predictions performed by a net of Bayesian Networks and logs data in a cloud (limited) free-storage provider. 

More specifically, room sensors and lamps are connected to the flat's wireless LAN with the help of 2 ESP8266 devices, which were programmed via the Arduino interface (codes: BedroomLight.ino and BedroomTableLamp.ino). 
Also, a Multi-Agent System was set up to run on a Raspberry Pi 3 Model B through the JADE framework. There is an agent to interface with the room's sensors (PresenceAgent.java), an agent to interface with the lamp devices and run the Bayesian Network for predictions (LightAgent.java), and additionally an agent to spececifically interact with the hardware devices (ESP8266.java) and another one to interact with the cloud provider (DataBaseAgent.java). 
By the time the project took place, the Bayesian Network libraries available in java did not run with the RPi's ARM processor architecture, so the Bayesian Network was coded in python (pythonscriptbayesiannetwork.py). 
There's also a script (myscript.sh) to speed up java compilations and to start JADE. 

More details can be found in the final report, in portuguese: 
https://www.dropbox.com/s/tnfrlzti8occxfq/TCC%20Murilo%20Provenzi-p%C3%B3s-banca-%20v5.3%20-%20final.pdf?dl=0

Further works to be performed, if ever, are the need to set up a system log and also add -much- more comments to the whole code.
