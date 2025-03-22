package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;
/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientUpdate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 *  Display patient profile for patient id.
	 */
	@GetMapping("/patient/edit/{id}")
	public String getUpdateForm(@PathVariable int id, Model model) {
		//test
		System.out.println("Fetching patient with ID: " + id);

		PatientView pv = new PatientView();
		// TODO search for patient by id
		//  if not found, return to home page using return "index"; 
		//  else create PatientView and add to model.
		// return editable form with patient data
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement("select * from patient where patient_id = ?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				//patient is found

				pv.setId(id);
				pv.setFirst_name(rs.getString("first_name"));
				pv.setLast_name(rs.getString("last_name"));
				pv.setStreet(rs.getString("street"));
				pv.setCity(rs.getString("city"));
				pv.setState(rs.getString("state"));
				pv.setZipcode(rs.getString("zip_code"));
				pv.setBirthdate(rs.getString("birth_date"));
				// we only have id of primary, we need to get the name from other table
				int PrimaryDoctorId = rs.getInt("primary_physician_id");
				ps = con.prepareStatement("select last_name from doctor where id = ?");
				ps.setInt(1, PrimaryDoctorId);
				rs = ps.executeQuery();
				if (rs.next()) {
					pv.setPrimaryName(rs.getString("last_name"));
				} else {
					model.addAttribute("message", "Problem finding doctor name");
					return "index";
				}

				model.addAttribute("message", "Patient found.");
				model.addAttribute("patient", pv);
				return "patient_edit";
			} else {
				//patient is not found
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", pv);
				return "index";
			}

		} catch(SQLException e) {
			model.addAttribute("message", "SQL ERROR: " + e.getMessage());
			return "index";
		}

}
	
	
	/*
	 * Process changes from patient_edit form
	 *  Primary doctor, street, city, state, zip can be changed
	 *  ssn, patient id, name, birthdate, ssn are read only in template.
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(PatientView p, Model model) {
		try (Connection con = getConnection();) {

      // validate doctor last name
      PreparedStatement ps = con.prepareStatement("Select id from doctor where last_name = ?");
      ps.setString(1, p.getPrimaryName());
      ResultSet rs = ps.executeQuery();
      int doctorId;
      if (rs.next()) {
        doctorId = rs.getInt("id");
      } else {
        model.addAttribute("message", "issue validating doctor");
        return "patient_edit";
      }

      // TODO update patient profile data in database
      PreparedStatement updatePS = con.prepareStatement(
          "update patient set street = ?, city = ?, state = ?, zip_code = ?, primary_physician_id=? where patient_id = ?");
      updatePS.setString(1, p.getStreet());
      updatePS.setString(2, p.getCity());
      updatePS.setString(3, p.getState());
      updatePS.setString(4, p.getZipcode());
      updatePS.setInt(5, doctorId);
			updatePS.setInt(6, p.getId());

			updatePS.executeUpdate();

			model.addAttribute("message", "Patient successfully updated");
			model.addAttribute("patient", p);
			return "patient_show";

    } catch(SQLException e) {
			model.addAttribute("message", "SQL ERROR: " + e.getMessage());
			model.addAttribute("patient", p);
			return "patient_edit";
		}

	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */
	private Connection getConnection() throws SQLException {
		return jdbcTemplate.getDataSource().getConnection();
	}


}
