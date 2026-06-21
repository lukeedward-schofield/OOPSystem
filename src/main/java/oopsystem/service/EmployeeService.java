package oopsystem.service;


import oopsystem.model.Employee;
import oopsystem.repository.ActivityLogRepository;
import oopsystem.repository.EmployeeRepository;

public class EmployeeService {

    private EmployeeRepository employeeRepository = new EmployeeRepository();
    private ActivityLogRepository activityLogRepository = new ActivityLogRepository();

    public Boolean terminateEmployee(Employee employee) {

        boolean isDeleted = employeeRepository.deleteEmployeeById(employee.getEmployeeId());

//        System.out.println(isDeleted);

        if (!isDeleted) {
            System.out.println("Error occurred when deleting employee. The problem might be in the repository");
            return false;
        }

        // Record the successful employee deletion in Activity Logs.
        activityLogRepository.log(
                "DELETE_EMPLOYEE",
                    String.format(
                            "Deleted employee: %s %s (Employee ID: %d, Department: %s)",
                            employee.getFirstName(),
                            employee.getLastName(),
                            employee.getEmployeeId(),
                            employee.getDepartment()));
        return true;

    }
}
