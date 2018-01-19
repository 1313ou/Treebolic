#!/bin/bash

source "../../../make-artwork-lib.sh"

launch="ic_launcher.svg ic_launcher_round.svg"
web="ic_launcher.svg"

icon="ic_treebolic.svg"
action="ic_action_*.svg"
prov="ic_xml.svg ic_textindent.svg ic_textindenttre.svg ic_textpair.svg ic_refresh.svg"
splash="ic_splash.svg"
splash_text="ic_splash_text.svg"

make_mipmap "${launch}" 48
make_app "${web}" 512

make_res "${icon}" 48
make_res "${action}" 24
make_res "${prov}" 48
make_res "${splash}" 144
make_res "${splash_text}" 12

