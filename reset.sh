#!/bin/sh

cd $HOME/Pictures
rm -v gdrive/* events/* show
ln -sv $HOME/Pictures/events show

