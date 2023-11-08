package dev.mr3n.basalt.exception

import org.apache.commons.lang3.ArchUtils

class ArchNotSupportedException(val archName: String = ArchUtils.getProcessor().arch.name): Exception("this architecture is not supported.")