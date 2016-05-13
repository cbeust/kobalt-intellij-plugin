package com.beust.kobalt.intellij

import com.beust.kobalt.intellij.server.ServerUtil
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.OSProcessManager

/**
 * @author Dmitry Zhuravlev
 *         Date:  13.05.2016
 */
class MyCapturingProcessHandler(commandLine: GeneralCommandLine) : CapturingProcessHandler(commandLine) {
    override fun killProcessTree(process: Process) {
        executeOnPooledThread({
            killProcessTreeSync(process);
        }
        )
    }

    private fun killProcessTreeSync(process: Process) {
        ServerUtil.LOG.debug("killing process tree")
        val destroyed = OSProcessManager.getInstance().killProcessTree(process)
        if (!destroyed) {
            if (isTerminated(process)) {
                ServerUtil.LOG.warn("Process has been already terminated: " + this.myCommandLine)
            } else {
                ServerUtil.LOG.warn("Cannot kill process tree. Trying to destroy process using Java API. Cmdline:\n" + this.myCommandLine)
                process.destroy()
            }
        }

    }

    private fun isTerminated(process: Process): Boolean {
        try {
            process.exitValue()
            return true
        } catch (var2: IllegalThreadStateException) {
            return false
        }

    }
}