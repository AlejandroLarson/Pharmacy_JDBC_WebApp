package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;


	/*
	 * Request blank patient registration form.
	 */
	@GetMapping("/patient/new")
	public String getNewPatientForm(Model model) {
		// return blank form for new patient registration
		model.addAttribute("patient", new PatientView());
		return "patient_register";
	}

	/*
	 * Process data from the patient_register form
	 */
	@PostMapping("/patient/new")
	public String createPatient(PatientView p, Model model) {

		/*
		 * validate doctor last name and find the doctor id
		 */
		// TODO
		String findDoctor = "SELECT id FROM doctor WHERE last_name = ?";

		List<Integer> doctorIds = jdbcTemplate.query(findDoctor, new Object[]{p.getPrimaryName()},
				(rs, rowNum) -> rs.getInt("id"));

		if (doctorIds.isEmpty()) {
			model.addAttribute("message", "Error: Doctor not found.");
			model.addAttribute("patient", p);
			return "patient_register"; // Show form again with error message
		}

		Integer doctorId = doctorIds.get(0); // Get the first matching doctor ID

		/*
		 * insert to patient table
		 */
		String insertPatient = "INSERT INTO patient (ssn, first_name, last_name, birth_date, street, city, state, zip_code, primary_physician_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

		jdbcTemplate.update(insertPatient, p.getSsn(), p.getFirst_name(), p.getLast_name(),
				java.sql.Date.valueOf(p.getBirthdate()), p.getStreet(), p.getCity(), p.getState(),
				p.getZipcode(), doctorId);

		String getPatientId = "SELECT LAST_INSERT_ID()";
		Integer patientId = jdbcTemplate.queryForObject(getPatientId, Integer.class);

		if (patientId == null) {
			model.addAttribute("message", "Error: Patient registration failed.");
			model.addAttribute("patient", p);
			return "patient_register";
		}

		p.setId(patientId);


		// display patient data and the generated patient ID,  and success message
		model.addAttribute("message", "Registration successful.");
		model.addAttribute("patient", p);
		return "patient_show";

		/*
		 * on error
		 * model.addAttribute("message", some error message);
		 * model.addAttribute("patient", p);
		 * return "patient_register";
		 */
	}

	/*
	 * Request blank form to search for patient by id and name
	 */
	@GetMapping("/patient/edit")
	public String getSearchForm(Model model) {
		model.addAttribute("patient", new PatientView());
		return "patient_get";
	}

	/*
	 * Perform search for patient by patient id and name.
	 */
	@PostMapping("/patient/show")
	public String showPatient(PatientView p, Model model) {

		// TODO   search for patient by id and name
		try (Connection con = getConnection();) {
			String findUserIdName = "SELECT * FROM patient WHERE patient_id = ? AND last_name = ?";
			PreparedStatement ps = con.prepareStatement(findUserIdName);
			ps.setInt(1, p.getId());
			ps.setString(2, p.getLast_name());


			// if found, return "patient_show", else return error message and "patient_get"
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				p.setFirst_name(rs.getString("first_name"));
				p.setLast_name(rs.getString("last_name"));
				p.setBirthdate(rs.getString("birth_date"));
				p.setStreet(rs.getString("street"));
				p.setCity(rs.getString("city"));
				p.setState(rs.getString("state"));
				p.setZipcode(rs.getString("zip_code"));
				int PrimaryDoctorId = rs.getInt("primary_physician_id");
				ps = con.prepareStatement("select last_name from doctor where id = ?");
				ps.setInt(1, PrimaryDoctorId);
				rs = ps.executeQuery();
				if (rs.next()) {
					p.setPrimaryName(rs.getString("last_name"));
				} else {
					model.addAttribute("patient", p);
					model.addAttribute("message", "Problem finding doctor name");
					return "patient_get";
				}
				model.addAttribute("patient", p);
				model.addAttribute("message", "Patient found.");
				return "patient_show";

			}
			else {
				model.addAttribute("message", "Error: Patient not found.");
				model.addAttribute("patient", p);
				return "patient_get";
			}

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";
		}

	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}
