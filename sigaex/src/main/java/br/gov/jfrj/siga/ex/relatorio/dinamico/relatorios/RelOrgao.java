package br.gov.jfrj.siga.ex.relatorio.dinamico.relatorios;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.TemporalType;

import net.sf.jasperreports.engine.JRException;

import org.hibernate.Query;

import ar.com.fdvs.dj.domain.builders.DJBuilderException;
import ar.com.fdvs.dj.domain.constants.Font;
import br.gov.jfrj.relatorio.dinamico.AbstractRelatorioBaseBuilder;
import br.gov.jfrj.relatorio.dinamico.RelatorioRapido;
import br.gov.jfrj.relatorio.dinamico.RelatorioTemplate;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.model.dao.HibernateUtil;

public class RelOrgao extends RelatorioTemplate {

	public RelOrgao(Map parametros) throws DJBuilderException {
		super(parametros);
		if (parametros.get("secaoUsuario") == null) {
			throw new DJBuilderException(
					"Parâmetro secaoUsuario não informado!");
		}
		if (parametros.get("lotacaoTitular") == null) {
			throw new DJBuilderException("Parâmetro lotação não informado!");
		}
		if (parametros.get("orgao") == null) {
			throw new DJBuilderException("Parâmetro órgão não informado!");
		}
		//if (parametros.get("lotacao") == null) {
		//	throw new DJBuilderException("Parâmetro órgão não informado!");
		//}
		if (parametros.get("dataInicial") == null) {
			throw new DJBuilderException("Parâmetro dataInicial não informado!");
		}
		if (parametros.get("dataFinal") == null) {
			throw new DJBuilderException("Parâmetro dataFinal não informado!");
		}
		if (parametros.get("link_siga") == null) {
			throw new DJBuilderException("Parâmetro link_siga não informado!");
		}
	}

	@Override
	public AbstractRelatorioBaseBuilder configurarRelatorio()
			throws DJBuilderException, JRException {
		
		this.setTitle("Relatório de Despachos e Transferências");
		estiloTituloColuna.setFont(new Font(8,"Arial",true));
		this.addColuna("Lotação", 10, RelatorioRapido.CENTRO, false);
		this.addColuna("Expedientes recebidos", 10, RelatorioRapido.CENTRO,
				false);
		this.addColuna("Expedientes transferidos", 13,
				RelatorioRapido.CENTRO, false);
		this.addColuna("Expedientes arquivados", 15,
				RelatorioRapido.CENTRO, false);
		this.addColuna("Expedientes desarquivados", 13,
				RelatorioRapido.CENTRO, false);
		this.addColuna("Processos recebidos", 10, RelatorioRapido.CENTRO,
				false);
		this.addColuna("Processos transferidos", 13, RelatorioRapido.CENTRO,
				false);
		this.addColuna("Processos arquivados", 10,
				RelatorioRapido.CENTRO, false);
		this.addColuna("Processos desarquivados", 13,
				RelatorioRapido.CENTRO, false);
		return this;

	}

	@Override
	public Collection processarDados() throws Exception {

		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		List<String> d = new ArrayList<String>();

		if (parametros.get("lotacao").equals("")) {
			Query query = HibernateUtil
			.getSessao()
			.createQuery( "select mov.lotaCadastrante.siglaLotacao, mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.descTipoFormaDoc, "
							+ "mov.exTipoMovimentacao.descrTipoMovimentacao, count(distinct mob.idMobil.exDocumento.idDoc) "
							+ "from ExMovimentacao mov inner join mov.exMobil mob "
							+ "where mov.lotaCadastrante.orgaoUsuario.idOrgaoUsu = :orgaoUsu " 
							+ "and mov.exTipoMovimentacao in (3,6,4,9,21) "
							+ "and mov.exMovimentacaoCanceladora is null "
							+ "and mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.idTipoFormaDoc in (1,2) "
							+ "and mov.dtIniMov >= :dtini " 
							+ "and mov.dtIniMov <= :dtfim " 
							+ "group by mov.lotaCadastrante.siglaLotacao, " 
							+ "mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.descTipoFormaDoc, " 
							+ "mov.exTipoMovimentacao.descrTipoMovimentacao");
			
			Long orgaoUsu = Long.valueOf((String) parametros.get("orgao"));
			query.setLong("orgaoUsu", orgaoUsu);
			Date dtini = formatter.parse((String) (parametros.get("dataInicial") + " 00:00:00"));
			query.setTimestamp("dtini", dtini);
			Date dtfim = formatter.parse((String) (parametros.get("dataFinal") + " 23:59:59"));
			query.setTimestamp("dtfim", dtfim);

			SortedSet<String> set = new TreeSet<String>();
			TreeMap<String, Long> map = new TreeMap<String, Long>();

			Iterator it = query.list().iterator();
			while (it.hasNext()) {
				Object[] obj = (Object[]) it.next();
				String lotacao = (String) obj[0];
				String tipodoc = (String) obj[1];
				String tipomov = (String) obj[2];
				Long totaldesp = Long.valueOf(obj[3].toString());
				set.add(lotacao);
				map.put(chave(lotacao, tipodoc, tipomov), totaldesp);
			}
			for (String s : set) {
				d.add(s);
				acrescentarColuna(d, map, s, "Expediente", "Recebimento");
				acrescentarColuna(d, map, s, "Expediente", "Transferência");
				acrescentarColuna(d, map, s, "Expediente", "Arquivamento Corrente"); 
				acrescentarColuna(d, map, s, "Expediente", "Desarquivamento");
				acrescentarColuna(d, map, s, "Processo Administrativo",
				"Recebimento");
				acrescentarColuna(d, map, s, "Processo Administrativo",
						"Transferência");
				acrescentarColuna(d, map, s, "Processo Administrativo", "Arquivamento Corrente");
				acrescentarColuna(d, map, s, "Processo Administrativo", "Desarquivamento");
			}
		} else {
			Query query = HibernateUtil
					.getSessao()
					.createQuery( "select mov.lotaCadastrante.siglaLotacao, mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.descTipoFormaDoc, "
							+ "mov.exTipoMovimentacao.descrTipoMovimentacao, count(distinct mob.idMobil.exDocumento.idDoc) "
							+ "from ExMovimentacao mov inner join mov.exMobil mob "
							+ "where mov.lotaCadastrante.orgaoUsuario.idOrgaoUsu = :orgaoUsu " 
							+ "and mov.lotaCadastrante.idLotacao in (select l.idLotacao from DpLotacao as l where l.idLotacaoIni = :lotacaodest) "
							+ "and mov.exTipoMovimentacao in (3,6,4,9,21) "
							+ "and mov.exMovimentacaoCanceladora is null "
							+ "and mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.idTipoFormaDoc in (1,2) "
							+ "and mov.dtIniMov >= :dtini " 
							+ "and mov.dtIniMov <= :dtfim " 
							+ "group by mov.lotaCadastrante.siglaLotacao, " 
							+ "mob.idMobil.exDocumento.exFormaDocumento.exTipoFormaDoc.descTipoFormaDoc, " 
							+ "mov.exTipoMovimentacao.descrTipoMovimentacao");
			
			Long orgaoUsu = Long.valueOf((String) parametros.get("orgao"));
			query.setLong("orgaoUsu", orgaoUsu);
			
			// Obtém a lotação com o id passado...
			Query qrySetor = HibernateUtil.getSessao().createQuery(
					"from DpLotacao lot where lot.idLotacao = " + parametros.get("lotacao"));
						
			Set<DpLotacao> lotacaoSet = new HashSet<DpLotacao>();
			DpLotacao lotacaodest = (DpLotacao)qrySetor.list().get(0);
			lotacaoSet.add(lotacaodest);		
			
			query.setLong("lotacaodest", lotacaodest.getIdInicial());
			
			Date dtini = formatter.parse((String) (parametros.get("dataInicial") + " 00:00:00"));
			query.setTimestamp("dtini", dtini);
			Date dtfim = formatter.parse((String) (parametros.get("dataFinal") + " 23:59:59"));
			query.setTimestamp("dtfim", dtfim);
	
			SortedSet<String> set = new TreeSet<String>();
			TreeMap<String, Long> map = new TreeMap<String, Long>();

			Iterator it = query.list().iterator();
			while (it.hasNext()) {
				Object[] obj = (Object[]) it.next();
				String lotacao = (String) obj[0];
				String tipodoc = (String) obj[1];
				String tipomov = (String) obj[2];
				Long totaldesp = Long.valueOf(obj[3].toString());
				set.add(lotacao);
				map.put(chave(lotacao, tipodoc, tipomov), totaldesp);
			}
			for (String s : set) {
				d.add(s);
				acrescentarColuna(d, map, s, "Expediente", "Recebimento");
				acrescentarColuna(d, map, s, "Expediente", "Transferência");
				acrescentarColuna(d, map, s, "Expediente", "Arquivamento Corrente");
				acrescentarColuna(d, map, s, "Expediente", "Desarquivamento"); 
				acrescentarColuna(d, map, s, "Processo Administrativo","Recebimento");
				acrescentarColuna(d, map, s, "Processo Administrativo",	"Transferência");
				acrescentarColuna(d, map, s, "Processo Administrativo", "Arquivamento Corrente");
				acrescentarColuna(d, map, s, "Processo Administrativo", "Desarquivamento");
			}
		}
		return d;
	}

	private void acrescentarColuna(List<String> d, Map<String, Long> map,
			String s, String tipodoc, String tipomov) {
		Long l = 0L;
		String key = chave(s, tipodoc, tipomov);
		if (map.containsKey(key))
			l += map.get(key);

		if (tipomov.equals("Transferência")) {
			key = chave(s, tipodoc, "Despacho com Transferência");
			if (map.containsKey(key))
				l += map.get(key);
		}
		if (l > 0)
			d.add(l.toString());
		else
			d.add("-");
	}

	private String chave(String lotacao, String tipodoc, String tipomov) {
		return lotacao + tipodoc + tipomov;
	}
}
