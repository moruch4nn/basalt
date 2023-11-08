package dev.mr3n.basalt.exception

import org.apache.commons.lang3.SystemUtils

class OSNotSupportedException(val osName: String = SystemUtils.OS_NAME): Exception("this os is not supported.")