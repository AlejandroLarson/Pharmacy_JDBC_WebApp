package application;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests blank form for new prescription.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_create";
	}

	// process data entered on prescription_create form
	@PostMapping("/prescription")
	public String createPrescription(PrescriptionView p, Model model) {

		System.out.println("createPrescription " + p);

		try (Connection con = getConnection();) {
			PreparedStatement insertps = con.prepareStatement(
					"insert into prescription(doctor_id, patient_id, drug_id, quantity, number_of_refills, fill_date) "
							+ "values (?,?,?,?,?,?) ", PreparedStatement.RETURN_GENERATED_KEYS);

			/*
			 * valid doctor name and id
			 */
			//TODO
			PreparedStatement ps = con.prepareStatement("select * from doctor where id=? and last_name=? and first_name=?");
			ps.setInt(1, p.getDoctor_id());
			ps.setString(2, p.getDoctorLastName());
			ps.setString(3, p.getDoctorFirstName());

			ResultSet rs = ps.executeQuery();
			if(!rs.next()) {
				//invalid doctor
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
			insertps.setInt(1,  p.getDoctor_id());

			/*
			 * valid patient name and id
			 */
			//TODO
			ps = con.prepareStatement("select * from patient where patient_id=? and last_name=? and first_name=?");
			ps.setInt(1, p.getPatient_id());
			ps.setString(2, p.getPatientLastName());
			ps.setString(3, p.getPatientFirstName());

			rs = ps.executeQuery();
			if(!rs.next()) {
				//invalid patient
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
			insertps.setInt(2,  p.getPatient_id());

			/*
			 * valid drug name
			 */
			//TODO
			ps = con.prepareStatement("select * from drug where drug_name=?");
			ps.setString(1, p.getDrugName());

			rs = ps.executeQuery();
			if(!rs.next()) {
				//invalid drug
				model.addAttribute("message", "Drug not found.");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
			insertps.setInt(3, rs.getInt("drug_id"));

			ps.close();

			/*
			 * insert prescription
			 */
			//TODO

			insertps.setInt(4, p.getQuantity());
			insertps.setInt(5, p.getRefills());
			insertps.setDate(6, Date.valueOf(LocalDate.now().toString()));

			insertps.executeUpdate();
			rs = insertps.getGeneratedKeys();
			if(rs.next()) {
				p.setRxid(rs.getInt(1));
			}

			rs.close();
			insertps.close();

			model.addAttribute("message", "Prescription created.");
			model.addAttribute("prescription", p);
			return "prescription_show";
		} catch (SQLException e) {
			//TODO
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
