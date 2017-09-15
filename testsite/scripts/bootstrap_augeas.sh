#!/bin/bash

if [ ! -e /tmp/augeas-updated ]; then
	sudo add-apt-repository ppa:raphink/augeas
	sudo apt-get update
  	sudo apt-get -y install python-software-properties augeas-tools libaugeas-ruby augeas-lenses
  	touch /tmp/augeas-updated
fi