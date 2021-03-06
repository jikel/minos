package minos.model.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minos.model.bean.AssignationTribunal;
import minos.model.bean.Dossier;
import minos.model.bean.Jugement;
import minos.model.bean.RendezVous;
import minos.model.bean.Requete;
import minos.model.bean.RoleAdresse;

public class DossierDAO {

	private JugementDAO jugementDAO;
	private RequeteDAO requeteDAO;
	private AssignationTribunalDAO assignationTribunalDAO;
	private RendezVousDAO rendezVousDAO;

	public DossierDAO() {
		jugementDAO = new JugementDAO();
		requeteDAO = new RequeteDAO();
		assignationTribunalDAO = new AssignationTribunalDAO();
		rendezVousDAO = new RendezVousDAO();
	}

	public Dossier create() {
		PreparedStatement prepareStatement;
		try {
			prepareStatement = MinosConnection.getInstance().prepareStatement("INSERT INTO dossier () VALUES ()",
					Statement.RETURN_GENERATED_KEYS);
			prepareStatement.execute();
			ResultSet generatedKeys = prepareStatement.getGeneratedKeys();
			generatedKeys.next();
			long id = generatedKeys.getLong(1);
			return find(id);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Dossier updateAll(Dossier dossier) {
		updateNomsDocument(dossier);
		return find(dossier.getId());
	}

	public void updateNomsDocument(Dossier dossier) {
		try {
			PreparedStatement deleteStatement = MinosConnection.getInstance()
					.prepareStatement("DELETE FROM corr_dossier_document WHERE id_dossier = ?");
			deleteStatement.setLong(1, dossier.getId());
			deleteStatement.execute();

			// pour le keyset, on recupere l'equivalent de tous les numeros de
			// tiroir
			for (long idDocument : dossier.getNomsDocument().keySet()) {
				PreparedStatement insertStatement = MinosConnection.getInstance()
						.prepareStatement("INSERT INTO corr_dossier_document (id_dossier, id_document) VALUES (?, ?)");
				insertStatement.setLong(1, dossier.getId());
				insertStatement.setLong(2, idDocument);
				insertStatement.execute();

			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public Dossier find(long idDossier) {
		Dossier dossier = new Dossier(idDossier);

		// ajout de tous les jugements dans le dossier
		Collection<Jugement> jugements = jugementDAO.findJugementsForDossier(idDossier);
		dossier.setJugements(jugements);

		Collection<Requete> requetes = requeteDAO.findRequetesForDossier(idDossier);
		dossier.setRequetes(requetes);
		
		Collection<AssignationTribunal> assignationsTribunals = assignationTribunalDAO.findAssignationsTribunalForDossier(idDossier);
		dossier.setAssignationsTribunal(assignationsTribunals);
		
		Collection<RendezVous> rendezVous = rendezVousDAO.findRendezVousForDossier(idDossier);
		dossier.setRendezVous(rendezVous);

		ResultSet result;
		try {
			PreparedStatement prepareStatement = MinosConnection.getInstance()
					.prepareStatement("SELECT document.id, document.nom FROM document, corr_dossier_document "
							+ "WHERE document.id = corr_dossier_document.id_document AND corr_dossier_document.id_dossier = ?");
			prepareStatement.setLong(1, idDossier);
			result = prepareStatement.executeQuery();
			Map<Long, String> documents = new HashMap<>();
			while (result.next()) {
				long idDocument = result.getLong("id");
				String nom = result.getString("nom");
				documents.put(idDocument, nom);
			}
			dossier.setNomsDocument(documents);
			return dossier;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Collection<Dossier> tousLesDossiers() {
		Collection<Dossier> dossiers = new ArrayList<>();
		ResultSet result;
		try {
			PreparedStatement prepareStatement = MinosConnection.getInstance()
					.prepareStatement("SELECT id FROM dossier");
			result = prepareStatement.executeQuery();
			while (result.next()) {
				long idDossier = result.getLong("id");
				Dossier dossier = find(idDossier);
				dossiers.add(dossier);
			}
			return dossiers;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
