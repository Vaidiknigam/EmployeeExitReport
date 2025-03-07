# Employee Exit Automation

## Problem Statement
When an employee leaves the company (on their Last Working Day, or LWD), the HR team currently has to manually email all system stakeholders to deactivate the employee’s access to various systems. This process:
- Requires significant manual effort.
- Is prone to human error (e.g., forgetting to deactivate a system).
- Involves delays in ensuring complete access revocation.

## Solution Overview
The goal is to automate this process using a **Master API** that handles the deactivation of employees from various systems when their Active Directory (AD) profile is deactivated.

## Master API Workflow
1. **Trigger**: The Master API is activated when an employee’s AD profile is turned off.
2. **Deactivation Process**:
   - The API calls the deactivation APIs of all relevant systems asynchronously.
   - If a system’s API fails, it retries 2 times before marking it as failed.
3. **Email Notification**:
   - Sends an email to the HR and IT teams along with the employee’s reporting manager (if applicable).
   - The email contains a detailed status report of the deactivation process.
4. **Report Generation**:
   - A summary table is created showing the deactivation status for each system.

### Example Report Table:
| Employee ID | System      | Status      |
|-------------|-------------|-------------|
| UGLXXXX     | SkaleUp     | Deactivated |
| UGLXXXX     | GroXstream  | NA          |
| UGLXXXX     | Jayam       | Error       |

### Sample Email Format:
```
Subject: Exit Data/Exit Clearance - March 03, 2025  
Dear Team,  
Please find the exits and their status for today:

[Table with status details]

Thanks and Regards,  
Team UGRO
```

## Session Management Enhancement
### Problem:
Even after AD deactivation (e.g., at 7:30 PM), an employee who is already logged into a system (e.g., at 7:25 PM) can continue using it until they manually log out.

### Solution:
1. **Option A**: Implement session expiration where a new access token is generated every 30 minutes. If AD is off, the system denies the token, logging out the user.
2. **Option B**: When the Master API deactivates an employee, it forces all active sessions to terminate immediately.

## Integration Requirements for Individual Systems
Each system needs modifications to integrate with the Master API and enhance session management:

| System        | Required Updates                                |
|--------------|-----------------------------------------------|
| SkaleUp      | Implement session management & deactivation API |
| GroXstream   | Build a wrapper API for deactivation            |
| Jayam        | Develop admin-based API with session handling   |
| Easebuzz     | Obtain API from third-party provider            |
| Nach         | Develop a wrapper API & implement session control |
| Partner Portal | Build wrapper API for deactivation            |
| IT GOV       | Investigate feasibility of wrapper API          |
| DMS          | Develop API for deactivation & session handling  |
| SCF          | Create API to update status & manage sessions  |
| GRO PROTECT  | Develop API for deactivation & session control |

## Key Benefits
✅ Eliminates manual email-based deactivation.  
✅ Reduces errors through automation.  
✅ Ensures employees are logged out immediately post-AD deactivation.  
✅ Generates detailed reports for HR and IT tracking.  

## Technical Implementation
### API Call Example (JSON Payload):
```json
{
  "employeeId": "UGLXXXX",
  "email": "employee@company.com",
  "deactivationDate": "2025-03-06T18:00:00Z"
}
```

### Expected API Response:
```json
{
  "status": "Success",
  "details": [
    { "system": "SkaleUp", "status": "Deactivated" },
    { "system": "GroXstream", "status": "NA" },
    { "system": "Jayam", "status": "Error" }
  ]
}
```

## Deployment Instructions
1. Clone the repository:
   ```sh
   git clone https://github.com/YourRepo/EmployeeExitAutomation.git
   ```
2. Navigate to the project directory:
   ```sh
   cd EmployeeExitAutomation
   ```
3. Build the project:
   ```sh
   mvn clean install
   ```
4. Run the application:
   ```sh
   mvn spring-boot:run
   ```
5. Verify the API by testing an employee deactivation request.

---

### Contact & Support
For any issues or enhancements, please raise an issue in the GitHub repository or contact the IT support team.

📧 **Support Email**: support@ugrocapital.com   
---

🚀 **Automating Employee Exit Process - Making IT Secure & Efficient!**

