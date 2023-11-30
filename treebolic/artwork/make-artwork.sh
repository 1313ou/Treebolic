#!/bin/bash

source "./lib-artwork.sh"

launch="ic_launcher.svg ic_launcher_round.svg"
app="ic_launcher.svg"
logo="logo_app.svg"

icon="ic_treebolic.svg"
status="ic_status_*.svg"
provider="ic_xml.svg ic_textindent.svg ic_textindenttre.svg ic_textpair.svg ic_dot.svg ic_refresh.svg"
splash="ic_splash.svg"
splash_text="ic_splash_text.svg"

make_mipmap "${launch}" 48
make_app "${app}" 512
make_res "${logo}" 48

make_res "${icon}" 48
make_res "${status}" 24
make_res "${provider}" 48
make_res "${splash}" 144
make_res "${splash_text}" 12

check_dir "${dirres}"
