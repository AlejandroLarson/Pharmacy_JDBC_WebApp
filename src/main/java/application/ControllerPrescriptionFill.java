package application;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionFill {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
	 * Patient requests form to fill prescription.
	 */
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_fill";
	}

	// process data from prescription_fill form
	@PostMapping("/prescription/fill")
	public String processFillForm(PrescriptionView p, Model model) {
		try (Connection con = getConnection();) {
			PreparedStatement insertps = con.prepareStatement(
					"insert into prescription_fill(prescription_id,pharmacy_id,fill_date,price,refills_remaining) values(?,?,?,?,?)");


			/*
			 * valid pharmacy name and address, get pharmacy id and phone
			 */
			// TODO
			PreparedStatement queryps = con.prepareStatement("select * from pharmacy where pharmacy_name = ? and address = ?");
			queryps.setString(1, p.getPharmacyName());
			queryps.setString(2, p.getPharmacyAddress());
			ResultSet rs = queryps.executeQuery();

			if(rs.next()) {
				//is valid
				insertps.setInt(2, rs.getInt("pharmacy_id"));
				p.setPharmacyID(rs.getInt("pharmacy_id"));
				p.setPharmacyPhone(rs.getString("phone"));
			} else {
				model.addAttribute("message", "Pharmacy not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			// TODO find the prescription
			queryps = con.prepareStatement("select * from prescription where rxid = ?");
			queryps.setInt(1, p.getRxid());
			int drugId;
			rs = queryps.executeQuery();
			if(rs.next()) {
				p.setDoctor_id(rs.getInt("doctor_id"));
				p.setPatient_id(rs.getInt("patient_id"));
				p.setQuantity(rs.getInt("quantity"));
				p.setRefills(rs.getInt("number_of_refills"));
				drugId = rs.getInt("drug_id");
				insertps.setInt(1,p.getRxid());
			} else {
				model.addAttribute("message", "Prescription not found");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			// finding drug
			queryps = con.prepareStatement("select drug_name from drug where drug_id = ?");
			queryps.setInt(1, drugId);
			rs = queryps.executeQuery();

			if (rs.next()) {
				p.setDrugName(rs.getString("drug_name"));
			}

			// TODO find the patient information
			int patientId;
			queryps = con.prepareStatement("select * from patient where patient_id = ?");
			queryps.setInt(1, p.getPatient_id());
			rs = queryps.executeQuery();
			if(rs.next()) {
				if(!rs.getString("last_name").equals(p.getPatientLastName())){
					model.addAttribute("message", "Prescription/Person mismatch");
					model.addAttribute("prescription", p);
					return "prescription_fill";
				}
				p.setPatientFirstName(rs.getString("first_name"));
			} else {
				model.addAttribute("message", "Person not found.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			/*
			 * have we exceeded the number of allowed refills
			 * the first fill is not considered a refill.
			 */

			// TODO
			// first check if we are on the first fill
			boolean isFirstFill = true;
			queryps = con.prepareStatement(
					"select * from prescription_fill where prescription_id =? order by fill_id desc limit 1");
			queryps.setInt(1, p.getRxid());
			rs = queryps.executeQuery();
			if(rs.next()) {
				isFirstFill = false;
				// find how many refills we have left
				//p.setRefillsRemaining(rs.getInt("refills_remaining"));
				if(rs.getInt("refills_remaining") == 0) {
					model.addAttribute("message", "No refills remaining for this prescription");
					model.addAttribute("prescription", p);
					return "prescription_fill";
				} else {
					int refillsRemaining = rs.getInt("refills_remaining") - 1;
					insertps.setInt(5, refillsRemaining);
					p.setRefillsRemaining(refillsRemaining);
					p.setRefills(refillsRemaining);
				}
			}

			if (isFirstFill) {
				insertps.setInt(5, p.getRefills());
			}

			/*
			 * get doctor information
			 */
			// TODO
			queryps = con.prepareStatement("select * from doctor where id=?");
			queryps.setInt(1, p.getDoctor_id());
			rs = queryps.executeQuery();
			if(rs.next()) {
				p.setDoctorFirstName(rs.getString("first_name"));
				p.setDoctorLastName(rs.getString("last_name"));
			}

			/*
			 * calculate cost of prescription
			 */
			// TODO
			queryps = con.prepareStatement("select price from price_of_drug where drug_id = ? and pharmacy_id = ? and amount = ? ");
			queryps.setInt(1, drugId);
			queryps.setInt(2, p.getPharmacyID());
			queryps.setInt(3, p.getQuantity());
			rs = queryps.executeQuery();
			if(rs.next()) {
				insertps.setBigDecimal(4, rs.getBigDecimal("price"));
				p.setCost(rs.getBigDecimal("price").toString());

			} else {
				model.addAttribute("message", "Could not find drug at this pharmacy at this quantity");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}


			// TODO save updated prescription
			//save date
			Date currentDate = Date.valueOf(LocalDate.now().toString());
			insertps.setDate(3, currentDate);
			p.setDateFilled(currentDate.toString());
			//insert
			insertps.executeUpdate();


			// show the updated prescription with the most recent fill information
			model.addAttribute("message", "Prescription filled.");
			model.addAttribute("prescription", p);

			insertps.close();
			queryps.close();
			rs.close();
			return "prescription_show";
		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}
	}
	
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}