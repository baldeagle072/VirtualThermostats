/**
 *  Virtual Thermostat Control
 *
 *  Copyright 2015 Eric Roberts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Virtual Thermostat Control",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "To control a virtual thermostat device",
    category: "Green Living",
    parent: "baldeagle072:Virtual Thermostat Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Virtual Thermostat") {
		input "vTstat", "device.virtualThermostat", title: "Virtual Thermostat", required: true, multiple: false
	}
    section("Sensors") {
    	input "temperatureSensor", "capability.temperatureMeasurement", title: "Temperature Sensor", required: true, multiple: false
        input "humiditySensor", "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: false, multiple: false
    }
    section("Devices") {
    	input "ac", "capability.switch", title: "Air Conditioners?", required: false, multiple: true
        input "heater", "capability.switch", title: "Heaters?", required: false, multiple: true
        input "thermostat", "capability.switch", title: "Real Thermostats?", required: false, multiple: true  
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(temperatureSensor, "temperature", temperatureHandler)
    if (humiditySensor) {
    	subscribe(humiditySensor, "humidity", humidityHandler) 
    }
    if (thermostat) {
    	subscribe(thermostat, "heatingSetpoint", thermostatHeatingSetpointHandler)
        subscribe(thermostat, "coolingSetpoint", thermostatCoolingSetpointHandler)
        subscribe(thermostat, "thermostatMode", thermostatModeHandler)
    }
    subscribe(vTstat, "heatingSetpoint", vTstatHeatingSetpointHandler)
    subscribe(vTstat, "coolingSetpoint", vTstatCoolingSetpointHandler)
    subscribe(vTstat, "thermostatMode", vTstatModeHandler)
    vTstat.setHeatingSetpoint(62)
    vTstat.setCoolingSetpoint(78)
    vTstat.setMode("off")
    vTstat.setThermostatFanMode("off")
    vTstat.setState("idle")
    vTstat.temperature(temperatureSensor.latestValue("temperature"))
    if (humiditySensor) {
    	vTstat.humidity(humiditySensor.currentValue("humidity"))
    }
}

def temperatureHandler(evt) {
	log.debug "Temperature: ${evt.value}"
    def temp = temperatureSensor.latestValue("temperature")
    vTstat.temperature(temp)
    checkTemp()
}

def humidityHandler(evt) {
	log.debug "Humidity: ${evt.value}"
    def humidity = evt.value
    vTstat.humidity(humidity)
}

def thermostatHeatingSetpointHandler(evt) {
	if (evt.isPhysical) {
    	log.debug "Setting Heating Setpoint from Thermostat: ${evt.value}"
		vTstat.setHeatingSetpoint(evt.value)
    }
}

def thermostatCoolingSetpointHandler(evt) {
	if (evt.isPhysical) {
    	log.debug "Setting Cooling Setpoint from Thermostat: ${evt.value}"
		vTstat.setCoolingSetpoint(evt.value)
    }
}

def thermostatModeHandler(evt) {
	if (evt.isPhysical) {
    	log.debug "Setting Thermostat Mode from Thermostat: ${evt.value}"
    	vTstat.setThermostatMode(evt.value)
    }
}

def vTstatHeatingSetpointHandler(evt) {
	log.debug("heatingSetpoint: ${evt.value}")
    checkTemp()
    if (thermostat) {
    	thermostat.setHeatingSetpoint(evt.value)
    }
}

def vTstatCoolingSetpointHandler(evt) {
	log.debug("coolingSetpoint: ${evt.value}")
	checkTemp()
    if (thermostat) {
    	thermostat.setCoolingSetpoint(evt.value)
    }
}

def vTstatModeHandler(evt) {
	log.debug("thermostatMode: ${evt.value}")
	turnOff()
    checkTemp()
    if (thermostat) {
    	thermostat.setThermostatMode(evt.value)
    }
}

def checkTemp() {
	def temperature = temperatureSensor.latestValue("temperature")
    def heatingSetpoint = vTstat.latestValue("heatingSetpoint")
    def coolingSetpoint = vTstat.latestValue("coolingSetpoint")
    def thermostatMode = vTstat.latestValue("thermostatMode")
    
    log.debug("Check Temp; temperature: $temperature, heatingSetpoint: $heatingSetpoint, coolingSetpoint: $coolingSetpoint, thermostatMode: $thermostatMode") 
    
    if (thermostatMode == "cool") {
        checkAC(temperature, coolingSetpoint)
    } else if (thermostatMode == "heat") {
    	checkHeater(temperature, heatingSetpoint)
    } else if (thermostatMode == "auto") {
    	checkAC(temperature, coolingSetpoint)
        checkHeater(temperature, heatingSetpoint)
    } else {
    	turnOff()
    } 
    
    checkState()
}

def checkAC (temperature, coolingSetpoint) {
	log.debug "checkAC(); temperature: $temperature, coolingSetpoint: $coolingSetpoint"
	if (temperature > coolingSetpoint + 0.5) {
       	//turn on AC
        if (ac) {
        	ac.on()
        }
        state.cooling = true
    } else if (temperature < coolingSetpoint) {
        //turn off AC
        if (ac) {
        	ac.off()
        }
        state.cooling = false
    }
}

def checkHeater (temperature, heatingSetpoint) {
	log.debug "checkHeater(); temperature: $temperature, heatingSetpoint: $heatingSetpoint"
    if (temperature <= heatingSetpoint - 1.0) {
       	//turn on heater
        if (heater) {
        	heater.on()
        }
        state.heating = true
    } else if (temperature >= heatingSetpoint + 1.0) {
        //turn off heater
        if (heater) {
        	heater.off()
        }
        state.heating = false
    }
}

def turnOff() {
	log.debug "turnOff()"
	//turn off heater
    if (heater) {
    	heater.off()
    }
    //turn off AC
    if (ac) {
    	ac.off()
    }
    state.cooling = false
    state.heating = false
}

def checkState() {
	if (state.cooling) {
    	vTstat.setState("cooling")
        log.debug("cooling")
    } else if (state.heating) {
    	vTstat.setState("heating")
        log.debug("heating")
    } else {
    	vTstat.setState("idle")
        log.debug("idle")
    }
}