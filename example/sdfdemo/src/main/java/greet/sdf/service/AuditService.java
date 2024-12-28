package greet.sdf.service;

import com.simplj.di.annotations.Dependency;

import java.time.LocalDateTime;

@Dependency
public class AuditService {
    private final StringBuilder sb;

    public AuditService() {
        this.sb = new StringBuilder("Audit Logs:\n============");
    }

    public void log(String msg) {
        sb.append("\n").append(LocalDateTime.now()).append(": ").append(msg);
    }

    public void printAuditLogs() {
        System.out.println(sb.toString());
    }
}
