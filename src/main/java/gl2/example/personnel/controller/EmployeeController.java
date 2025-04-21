package gl2.example.personnel.controller;



import gl2.example.personnel.model.Employee;
import gl2.example.personnel.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = employeeService.getEmployeeById(id);
        if (employee.isPresent()) {
            return ResponseEntity.ok(employee.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Employee addEmployee(@RequestBody Employee employee) {
        return employeeService.addEmployee(employee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee employee) {

        // Optionnel : vérifier si l'employé avec l'ID donné existe
        Optional<Employee> existingEmployee = employeeService.getEmployeeById(id);
        if (existingEmployee.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Mettre à jour l'employé avec les nouvelles informations
        employee.setId(id);  // S'assurer que l'ID de l'objet est le même que celui dans l'URL
        Employee updatedEmployee = employeeService.addEmployee(employee);

        return ResponseEntity.ok(updatedEmployee);
    }
}


