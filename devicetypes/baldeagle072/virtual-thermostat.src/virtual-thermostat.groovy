/*
 *  Virtual Thermostat multi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License  for the specific language governing permissions and limitations
 *  under the License.
 *
 *  This work is a combination of projects by mulitple contributers.
 */

metadata{
  definition(name:"Virtual Thermostat", namespace:"baldeagle072", author:"Eric Roberts"){
    capability "Temperature Measurement"
    capability "Relative Humidity Measurement"
    capability "Thermostat"
    capability "Sensor"

    // custom commands
    command "heatLevelUp"
    command "heatLevelDown"
    command "coolLevelUp"
    command "coolLevelDown"
    command "setMode"
    command "switchMode"
    command "switchFanMode"
    command "temperature"
    command "humidity"
    command "setState"
    command "setTemperature"
  }

  tiles(scale: 2){
  	multiAttributeTile(name:"thermostatMulti", type:"thermostat", width:6, height:4) {
      tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
        attributeState("default", label:'${currentValue}°')
      }
      tileAttribute("device.temperature", key: "VALUE_CONTROL") {
        attributeState("default", action: "setTemperature")
      }
      tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
        attributeState("default", label:'${currentValue}%', unit:"%")
      }
      tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
        attributeState("idle", backgroundColor:"#44b621")
        attributeState("heating", backgroundColor:"#ffa81e")
        attributeState("cooling", backgroundColor:"#269bd2")
      }
      tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
        attributeState("off", label:'${name}')
        attributeState("heat", label:'${name}')
        attributeState("cool", label:'${name}')
        attributeState("auto", label:'${name}')
      }
      tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
        attributeState("default", label:'${currentValue}', unit:"dF")
      }
      tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
        attributeState("default", label:'${currentValue}', unit:"dF")
      }
	}
    valueTile("temperature", "device.temperature", width: 1, height: 1) {
      state("temperature", label:'${currentValue}°', unit:"F",
        backgroundColors:[
          [value: 31, color: "#153591"],
          [value: 44, color: "#1e9cbb"],
          [value: 59, color: "#90d2a7"],
          [value: 74, color: "#44b621"],
          [value: 84, color: "#f1d801"],
          [value: 95, color: "#d04e00"],
          [value: 96, color: "#bc2323"]
        ]
      )
    }
    standardTile("mode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
        state "off", label:'', action:"switchMode", icon:"st.thermostat.heating-cooling-off"
        state "heat", label:'', action:"switchMode", icon:"st.thermostat.heat"
        state "cool", label:'', action:"switchMode", icon:"st.thermostat.cool"
        state "auto", label:'', action:"switchMode", icon:"st.thermostat.auto"
    }
    standardTile("fanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
        state "auto", label:'', action:"switchFanMode", icon:"st.thermostat.fan-auto"
        state "on", label:'', action:"switchFanMode", icon:"st.thermostat.fan-on"
        state "off", label:'', action:"switchFanMode", icon:"st.thermostat.fan-off"
    }
    
    valueTile("thermostatOperatingState", "device.thermostatOperatingState", width: 1, height: 1) {
        state "default", label:'${currentValue}', action:"", unit:"",
        backgroundColors:[
          [value: 'idle', color: "#ffffff"],
          [value: 'heating', color: "#f1d801"],
          [value: 'cooling', color: "#153591"],
          [value: 'circulate', color: "#1e9cbb"]
        ]
    }

    main("thermostatMulti")
    details(["thermostatMulti", "thermostatOperatingState", "mode", "fanMode"])
  }

    simulator{
      for(int i = 0; i <= 100; i += 20){
        status "Humidity ${i}%": "humidity:${i}"
      }

      for(int x = 0; x <= 100; x += 20){
        status "Temperature ${x}%": "temperature:${x}"
      }
    }
}

def setTemperature(value) {
	def increaseTemp = device.currentValue('temperature') < value
    def newHSetpoint
    def newCSetpoint
	switch (device.currentValue('thermostatMode')) {
    	case "heat":
        	increaseTemp ? (newHSetpoint = device.currentValue("heatingSetpoint") + 1) : (newHSetpoint = device.currentValue("heatingSetpoint") - 1)
            setHeatingSetpoint(newHSetpoint.toInteger())
            break
        case "cool":
        	increaseTemp ? (newCSetpoint = device.currentValue("coolingSetpoint") + 1) : (newCSetpoint = device.currentValue("coolingSetpoint") - 1)
            setCoolingSetpoint(newCSetpoint.toInteger())
            break
        case "auto":
        	increaseTemp ? (newHSetpoint = device.currentValue("heatingSetpoint") + 1) : (newHSetpoint = device.currentValue("heatingSetpoint") - 1)
            setHeatingSetpoint(newHSetpoint.toInteger())
            increaseTemp ? (newCSetpoint = device.currentValue("coolingSetpoint") + 1) : (newCSetpoint = device.currentValue("coolingSetpoint") - 1)
            setCoolingSetpoint(newCSetpoint.toInteger())
        	break
    }
}

def temperature(BigDecimal value){
	log.debug("Vstat Temp: $value")
  sendEvent(name: "temperature", value: value, unit: "F")
}

def humidity(BigDecimal value){
	log.debug("Vstat Humidity: $value")
  sendEvent(name: "humidity", value: value, unit: "%")
}

def coolLevelUp(){
    int nextLevel = device.currentValue("coolingSetpoint") + 1

    if( nextLevel > 99){
      nextLevel = 99
    }
    log.debug "Setting cool set point up to: ${nextLevel}"
    quickSetCool(nextLevel)
}

def coolLevelDown(){
    int nextLevel = device.currentValue("coolingSetpoint") - 1

    if( nextLevel < 50){
      nextLevel = 50
    }
    log.debug "Setting cool set point down to: ${nextLevel}"
    quickSetCool(nextLevel)
}

def heatLevelUp(){
    int nextLevel = device.currentValue("heatingSetpoint") + 1

    if( nextLevel > 90){
      nextLevel = 90
    }
    log.debug "Setting heat set point up to: ${nextLevel}"
    quickSetHeat(nextLevel)
}

def heatLevelDown(){
    int nextLevel = device.currentValue("heatingSetpoint") - 1

    if( nextLevel < 40){
      nextLevel = 40
    }
    log.debug "Setting heat set point down to: ${nextLevel}"
    quickSetHeat(nextLevel)
}

def quickSetHeat(degrees) {
  setHeatingSetpoint(degrees, 1000)
}

def setHeatingSetpoint(degrees, delay = 30000) {
  setHeatingSetpoint(degrees.toDouble(), delay)
}

def setHeatingSetpoint(Double degrees, Integer delay = 30000) {
  log.trace "setHeatingSetpoint($degrees, $delay)"
  def deviceScale = state.scale ?: 1
  def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
  def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    if (locationScale == "C" && deviceScaleString == "F") {
      convertedDegrees = celsiusToFahrenheit(degrees)
    } else if (locationScale == "F" && deviceScaleString == "C") {
      convertedDegrees = fahrenheitToCelsius(degrees)
    } else {
      convertedDegrees = degrees
    }

    sendEvent(name: "heatingSetpoint", value: convertedDegrees)
}

def quickSetCool(degrees) {
  setCoolingSetpoint(degrees, 1000)
}

def setCoolingSetpoint(degrees, delay = 30000) {
  setCoolingSetpoint(degrees.toDouble(), delay)
}

def setCoolingSetpoint(Double degrees, Integer delay = 30000) {
    log.trace "setCoolingSetpoint($degrees, $delay)"
  def deviceScale = state.scale ?: 1
  def deviceScaleString = deviceScale == 2 ? "C" : "F"
    def locationScale = getTemperatureScale()
  def p = (state.precision == null) ? 1 : state.precision

    def convertedDegrees
    if (locationScale == "C" && deviceScaleString == "F") {
      convertedDegrees = celsiusToFahrenheit(degrees)
    } else if (locationScale == "F" && deviceScaleString == "C") {
      convertedDegrees = fahrenheitToCelsius(degrees)
    } else {
      convertedDegrees = degrees
    }

  sendEvent(name: "coolingSetpoint", value: convertedDegrees)
}

def modes() {
  ["off", "heat", "cool", "auto"]
}

def switchMode() {
  def currentMode = device.currentState("mode")?.value
  def lastTriedMode = state.lastTriedMode ?: currentMode ?: "off"
  def supportedModes = getDataByName("supportedModes")
  def modeOrder = modes()
  def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
  def nextMode = next(lastTriedMode)
  if (supportedModes?.contains(currentMode)) {
    while (!supportedModes.contains(nextMode) && nextMode != "off") {
      nextMode = next(nextMode)
    }
  }
  state.lastTriedMode = nextMode
  switchToMode(nextMode)
}

def switchToMode(nextMode) {
  def supportedModes = getDataByName("supportedModes")
  if(supportedModes && !supportedModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"
  if (nextMode in modes()) {
    state.lastTriedMode = nextMode
    log.debug "changing mode: $nextMode"
    "$nextMode"()
  } else {
    log.debug("no mode method '$nextMode'")
  }
}

def switchFanMode() {
  def currentMode = device.currentState("thermostatFanMode")?.value
  def lastTriedMode = state.lastTriedFanMode ?: currentMode ?: "auto"
  log.debug "last tried: ${lastTriedMode}"
  def supportedModes = getDataByName("supportedFanModes") ?: "auto on"
  def modeOrder = ["auto", "on"]
  def next = { modeOrder[modeOrder.indexOf(it) + 1] ?: modeOrder[0] }
  def nextMode = next(lastTriedMode)
  while (!supportedModes?.contains(nextMode) && nextMode != "auto") {
    nextMode = next(nextMode)
  }

  switchToFanMode(nextMode)
}

def switchToFanMode(nextMode) {
  def supportedFanModes = getDataByName("supportedFanModes")
  if(supportedFanModes && !supportedFanModes.contains(nextMode)) log.warn "thermostat mode '$nextMode' is not supported"

  def returnCommand
  if (nextMode == "auto") {
    returnCommand = fanAuto()
  } else if (nextMode == "on") {
    returnCommand = fanOn()
  } else {
    log.debug("no fan mode '$nextMode'")
  }
  if(returnCommand) state.lastTriedFanMode = nextMode
  returnCommand
}

def getDataByName(String name) {
  state[name] ?: device.getDataValue(name)
}

def getModeMap() { [
  "off": 0,
  "heat": 1,
  "cool": 2,
  "auto": 3,
]}

def setMode(String value) {
  sendEvent(name: "thermostatMode", value: value)
}

def getFanModeMap() { [
  "auto": 0,
  "on": 1
]}

def setThermostatFanMode(String value) {
  sendEvent(name: "thermostatFanMode", value: value)
}

def off() {
	log.debug("OFF")
  sendEvent(name: "thermostatMode", value: "off")
}

def heat() {
	log.debug("HEAT")
  sendEvent(name: "thermostatMode", value: "heat")
}

def cool() {
  sendEvent(name: "thermostatMode", value: "cool")
}

def auto() {
  sendEvent(name: "thermostatMode", value: "auto")
}

def fanOn() {
  log.debug "fan on"
  sendEvent(name: "thermostatFanMode", value: "on")
}

def fanAuto() {
  log.debug "fan auto"
  sendEvent(name: "thermostatFanMode", value: "auto")
}

def fanOff() {
  log.debug "fan off -> auto"
  sendEvent(name: "thermostatFanMode", value: "auto")
}

def setState(String state){
  sendEvent(name: "thermostatOperatingState", value: state)
}

private getStandardDelay() {
  1000
}

private def TRACE(message) {
    log.debug message
}