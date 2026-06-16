package oopsystem.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class PassSlip {

    private final IntegerProperty passSlipId;
    private final IntegerProperty employeeId;
    private final IntegerProperty issuedBy;
    private final StringProperty  reason;
    private final StringProperty  destination;
    private final StringProperty  filePath;

    private LocalDateTime timeIn;
    private LocalDateTime timeOut;
    private Integer       estimatedDuration; // DB column: estimated_duration (set at issuance)
    private Integer       duration;          // DB column: duration (actual, computed on return)
    private String        status;            // DB enum: 'OUT' | 'IN' | 'OVERDUE'

    // Joined from employee table for display — not a DB column on pass_slip.
    private String employeeName;

    // -------------------------------------------------------------------------
    // Constructor — NEW pass slip at issuance.
    // estimatedDuration entered by staff. duration is null until employee returns.
    // status starts as 'OUT'.
    // -------------------------------------------------------------------------
    public PassSlip(
            int employeeId,
            int issuedBy,
            String reason,
            String destination,
            LocalDateTime timeOut,
            int estimatedDuration) {

        this.passSlipId        = new SimpleIntegerProperty(0);
        this.employeeId        = new SimpleIntegerProperty(employeeId);
        this.issuedBy          = new SimpleIntegerProperty(issuedBy);
        this.reason            = new SimpleStringProperty(reason);
        this.destination       = new SimpleStringProperty(destination);
        this.filePath          = new SimpleStringProperty(null);
        this.timeOut           = timeOut;
        this.estimatedDuration = estimatedDuration;
        this.duration          = null;
        this.status            = "OUT";
    }

    // -------------------------------------------------------------------------
    // Constructor — READING an existing record from the database.
    // -------------------------------------------------------------------------
    public PassSlip(
            int passSlipId,
            int employeeId,
            int issuedBy,
            String reason,
            String destination,
            String filePath,
            LocalDateTime timeIn,
            LocalDateTime timeOut,
            Integer estimatedDuration,
            Integer duration,
            String status) {

        this.passSlipId        = new SimpleIntegerProperty(passSlipId);
        this.employeeId        = new SimpleIntegerProperty(employeeId);
        this.issuedBy          = new SimpleIntegerProperty(issuedBy);
        this.reason            = new SimpleStringProperty(reason);
        this.destination       = new SimpleStringProperty(destination);
        this.filePath          = new SimpleStringProperty(filePath);
        this.timeIn            = timeIn;
        this.timeOut           = timeOut;
        this.estimatedDuration = estimatedDuration;
        this.duration          = duration;
        this.status            = status;
    }

    // Getters
    public int           getPassSlipId()       { return passSlipId.get();  }
    public int           getEmployeeId()        { return employeeId.get();  }
    public int           getIssuedBy()          { return issuedBy.get();    }
    public String        getReason()            { return reason.get();      }
    public String        getDestination()       { return destination.get(); }
    public String        getFilePath()          { return filePath.get();    }
    public LocalDateTime getTimeIn()            { return timeIn;            }
    public LocalDateTime getTimeOut()           { return timeOut;           }
    public Integer       getEstimatedDuration() { return estimatedDuration; }
    public Integer       getDuration()          { return duration;          }
    public String        getStatus()            { return status;            }
    public String        getEmployeeName()      { return employeeName;      }

    // Setters — only fields that change after issuance
    public void setTimeIn(LocalDateTime timeIn)     { this.timeIn       = timeIn;          }
    public void setDuration(Integer duration)        { this.duration     = duration;        }
    public void setStatus(String status)             { this.status       = status;          }
    public void setFilePath(String filePath)         { this.filePath.set(filePath);         }
    public void setEmployeeName(String name)         { this.employeeName = name;            }
}