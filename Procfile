# bower: npm install bower && ./node_modules/bower/bin/bower --allow-root install && echo "Installed js dependencies"
web: lein with-profile production do clean-tools, load-tools, trampoline ring server-headless
