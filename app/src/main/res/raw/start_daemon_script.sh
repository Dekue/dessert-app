# argument 1 must be the path to daemon to be started
# argument 2 must be the path to the config file to be used to start the daemon
# argument 3 must be the path to the additional library directory
# error values
ERROR_DAEMON_FILE_ERROR=64
ERROR_CONFIG_FILE_ERROR=65
ERROR_LIBRARY_PATH_ERROR=66
# test daemon file
if [ ! -x $1 ] ; then
	echo daemon $1 is not an executable
	exit $ERROR_DAEMON_FILE_ERROR
fi
# test config file
if [ ! -r $2 ] ; then
	echo config $2 is not readable
	exit $ERROR_CONFIG_FILE_ERROR
fi
# test daemon file
if [ ! -d $3 ] ; then
	echo library directory $3 is not a directory
	exit $ERROR_LIBRARY_PATH_ERROR
fi
# add library path to LD_LIBRARY_PATH
export LD_LIBRARY_PATH=$3:$LD_LIBRARY_PATH
# finally start the daemon
echo "Calling $1 $2 with LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
$1 $2
# exit and return the daemon exit code
exit