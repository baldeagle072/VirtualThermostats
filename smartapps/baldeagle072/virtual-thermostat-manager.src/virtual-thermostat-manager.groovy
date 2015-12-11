/**
 *  Virtual Thermostat Manager
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
    name: "Virtual Thermostat Manager",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "To create virtual thermostat devices that control plugin heaters and air conditioners",
    category: "Green Living",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page name:"pageThermostats"
    page name:"pageControllers"
    page name:"pageOptions"
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
	initialize()
}

def initialize() {
	state.installed = true
    
    log.debug "PARENT:  there are ${childApps.size()} child Apps:"

	childApps.each {child ->
//    	log.debug "child.name is ${child.name}."
		if ( child.name == "Virtual Thermostat Creator" ) {
			log.debug "child Creator: ${child.label}"
        } else if ( child.name == "Virtual Thermostat Control" ) {
	        log.debug "child Control: ${child.label}"        
        }    
    }
}

private def pageThermostats() {
	def pageProperties = [
    	name:		"pageThermostats",
        title:		"Create and/or edit Virtual Thermostats",
        nextPage:	"pageControllers",
        install:	false,
        uninstall:	state.installed
    ]
    
    return dynamicPage(pageProperties) {
    	section {
        	app(name: "childThermostats", appName: "Virtual Thermostat Creator", namespace: "baldeagle072", title: "Create a new Virtual Thermostat", multiple: true)
        }
    }
}

private def pageControllers() {
	def pageProperties = [
    	name:		"pageControllers",
        title:		"Create and/or edit the controller for your Virtual Thermostats",
        nextPage:	"pageOptions",
        install:	false,
        uninstall:	state.installed
    ]
    
    return dynamicPage(pageProperties) {
    	section {
        	app(name: "childControllers", appName: "Virtual Thermostat Control", namespace: "baldeagle072", title: "Create a new controller", multiple: true)
        }
    }
}

private def pageOptions() {  
	def pageProperties = [
        name        : "pageOptions",
        title       : "Options.",
        nextPage    : null,
        install     : true,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {

        section([title:"Options", mobileOnly:true]) {
           	label title:"Assign a name", required:false
        }

	}
}