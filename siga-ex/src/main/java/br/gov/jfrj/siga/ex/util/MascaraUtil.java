package br.gov.jfrj.siga.ex.util;

import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mvel2.MVEL;

import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.ex.SigaExProperties;

public class MascaraUtil {

	private static String MASK_IN;
	private static String MASK_OUT;
	private static String MASK_SHOW;

	// MASCARA ATUAL
	// private static String MASK_IN =
	// "([0-9]{0,2})\\.?([0-9]{2})?\\.?([0-9]{2})?\\.?([0-9]{2})?([A-Z])?";
	// private static String MASK_OUT = "%1$02d.%2$02d.%3$02d.%4$02d";

	// MASCARA ANTIGA
	// private static String MASK_IN = "([0-9]{0,2})\\.?([0-9]{3})?\\.?([0-9]{2})?";
	// private static String MASK_OUT = "%1$02d.%2$03d.%3$02d";

	private static MascaraUtil instancia;

	private MascaraUtil() {

	}

	public static synchronized MascaraUtil getInstance() {
		if (instancia == null) {
			MASK_IN = SigaExProperties.getExClassificacaoMascaraEntrada();
			MASK_OUT = SigaExProperties.getExClassificacaoMascaraSaida();
			MASK_SHOW = SigaExProperties.getExClassificacaoMascaraExibicao();
			instancia = new MascaraUtil();
		}
		return instancia;
	}

	/**
	 * Retorna Expressão regular com formato em que a classificação documental deve
	 * estar de acordo
	 * 
	 * @return - regex da classificacao documental
	 */
	public String getMascaraEntrada() {
		return MASK_IN;
	}

	/**
	 * Retorna o formato da máscara a ser produzida na saída (vide Formatter.java)
	 * 
	 * @return - máscara de saída
	 */
	public String getMascaraSaida() {
		return MASK_OUT;
	}

	/**
	 * Retorna o formato da máscara a ser produzida para exibição (vide
	 * Formatter.java)
	 * 
	 * @return - máscara de exibição
	 */
	public String getMascaraExibicao() {
		return MASK_SHOW;
	}

	public void setMascaraEntrada(String regex) {
		MASK_IN = regex;
	}

	public void setMascaraSaida(String formatter) {
		MASK_OUT = formatter;
	}

	public void setMascaraExibicao(String formatter) {
		MASK_SHOW = formatter;
	}

	@Deprecated
	/**
	 * Formata um texto que esteja de acordo com a mascara de entrada
	 * 
	 * @param texto - texto a ser formatado como codificacao de classificação
	 *              documental
	 * @return - codificacao formatado de acordo com mascaraSaida. <br/>
	 *         Retorna null em caso de problemas com entrada ou saída.
	 */
	public String formatar(String texto) {
		final String mascara = getMascaraSaida();
		return formatar(texto, mascara);
	}

	@Deprecated
	/**
	 * Formata um texto que esteja de acordo com a mascara de exibição
	 * 
	 * @param texto - texto a ser formatado como codificacao de classificação
	 *              documental
	 * @return - codificacao formatado de acordo com mascaraExibicao. <br/>
	 *         Retorna null em caso de problemas com entrada ou saída.
	 */
	public String formatarParaExibicao(String texto) {
		String mascara = getMascaraExibicao();
		if (mascara == null || mascara.length() == 0)
			mascara = getMascaraSaida();
		return formatar(texto, mascara);
	}

	/**
	 * Formata um texto que esteja de acordo com a mascara informada
	 * 
	 * @param texto   - texto a ser formatado como codificacao de classificação
	 *                documental
	 * @param mascara - string representando a máscara a ser utilizada. Se a máscara
	 *                começar com "(", então será considerada uma expressão MVEL,
	 *                caso o contrário, será utilizada em uma chamada ao Formatter.
	 * @return - codificacao formatado de acordo com a máscara. <br/>
	 *         Retorna null em caso de problemas com entrada ou saída.
	 */
	private String formatar(String texto, final String mascara) {
		if (getMascaraEntrada() == null || mascara == null || texto == null) {
			return null;
		}
		Pattern pe = Pattern.compile(getMascaraEntrada());
		Matcher me = pe.matcher(texto);
		if (me.find()) {
			Object[] grupos = new Object[me.groupCount()];
			for (int i = 0; i < me.groupCount(); i++) {
				try {
					// Atribui o valor do grupo, como um número inteiro, ao índice do array.
					grupos[i] = Integer.valueOf(me.group(i + 1));
				} catch (NumberFormatException e) {
					// Atribui o valor do grupo ao índice do array.
					grupos[i] = me.group(i + 1);
				}
			}

			if (mascara != null && mascara.startsWith("(")) {
				try {
					String eval = aplicarFormatacaoMvel(mascara, grupos, null);

					// Verifica o resultado da avaliação e lança uma exceção, em caso de valor nulo.
					if (eval == null) {
						throw new AplicacaoException("Problema na expressão: " + mascara);
					}

					// Retorna o texto formatado conforme o método de codificação configurado.
					return eval;
				} catch (Exception e) {
					throw new AplicacaoException("Problema na expressão: " + mascara);
				}
			}

			// Aplica a formatação de saída.
			Formatter f = new java.util.Formatter();
			String codificacao = f.format(mascara, grupos).toString();
			f.close();

			// Retorna o texto formatado conforme o método de codificação configurado.
			return codificacao;
		}

		return null;
	}

	private String aplicarFormatacaoMvel(final String mascara, Object[] grupos, String caractere) {
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("grupos", grupos);
		vars.put("caractere", caractere);

		// Avalia a expressão MVEL.
		return (String) MVEL.eval(mascara, vars);
	}

	/**
	 * FIXME Corrigir documentação, que retorna expressão regular.
	 * 
	 * Produz a máscara para consultar m nível de classificacao documental
	 * 
	 * @param nivel - Nível na hierarquia desejado. Baseado em um (1)
	 * @return - Máscara para consultar o nível (Ex: nível 2: "__.__.00.00")
	 */
	public String getMscTodosDoNivel(int nivel) {
		// Recupera a lista de expressões regulares que permitem identificar cada nível
		// do plano de classificação documental.
		List<String> niveis = listarNiveisRegExPlanoClassificacao();

		// Validação do nível informado.
		if (nivel < 1 || nivel > niveis.size())
			throw new IllegalArgumentException("O nível de classificação informado é inválido.");

		// Cálculo do índice do nível a ser recuperado.
		int indice = nivel - 1;
		// Retorno da expressão SQL correspondente ao nível informado.
		return niveis.get(indice);
	}

	/**
	 * Retorna a máscara no maior nível possível.
	 * 
	 * @return - Máscara do maior nível possível Ex:__.__.__.__
	 */
	public String getMscTodosDoMaiorNivel() {
		return getMscTodosDoNivel(6);
	}

	public String getMscTodosNiveis() {
		return getMascaraEntrada();
	}

	/**
	 * FIXME Corrigir documentação, para informar o retorno de expressões regulares.
	 * 
	 * Produz a máscara correspondente para obter os filhos da classificacao.
	 * 
	 * @param codigo       - Valor da Classificacao Documental
	 * @param nivelInicial - Nível na hierarquia desejado. Baseado em um (1)
	 * @param niveisAbaixo - boolean que indica se deve ser calculados os níveis
	 *                     inferiores ao nível inicial
	 * @return - Máscara para consultar os filhos (Ex1: <br/>
	 *         nível 2: "11.__.00.00" <br/>
	 *         Ex2: nível 2 com niveis abaixo: "11.__.__.__" )
	 */
	public String getMscFilho(String codigo, int nivelInicial, boolean niveisAbaixo) {
		// Validação do código de classificação informado.
		if (!validar(codigo))
			throw new IllegalArgumentException("O código de classificação informado é inválido.");

		// Validação da máscara de entrada da formatação da classificação documental.
		if (getMascaraEntrada() == null || getMascaraEntrada().equals(""))
			throw new IllegalStateException(
					"O sistema apresentou um estado inválido: é necessário cadastrar a propriedade referente à máscara de formatação dos dados de entrada da classificação documental.");

		// Validação da máscara de saída (MVEL) da formatação da classificação
		// documental.
		// if (getMascaraSaida() == null || getMascaraSaida().equals(""))
		// throw new IllegalStateException("O sistema apresentou um estado inválido: é
		// necessário cadastrar a propriedade referente à máscara de formatação dos
		// dados de saída da classificação documental.");

		// Recupera a lista de expressões regulares que permitem identificar cada nível
		// do plano de classificação documental.
		List<String> regexs = listarNiveisRegExPlanoClassificacao();

		if (regexs == null || regexs.isEmpty())
			throw new IllegalStateException(
					"O sistema apresentou um estado inválido: é necessário cadastrar as propriedades referentes às máscaras dos níveis (assuntos) definidos para o método de codificação da classificação documental.");

		StringBuffer regex = null;
		int indice = nivelInicial - 1;

		if (niveisAbaixo)
			regex = new StringBuffer(getMascaraEntrada());
		else
			regex = new StringBuffer(regexs.get(indice));//.replace("0","(0)").replace("_", "([0-9])?").replace(".","\\."));

		Matcher matcher = Pattern.compile(getMascaraEntrada()).matcher(codigo);

		if (!matcher.matches())
			return null;

		String replaceable = "([0-9])";

		for (int i = 0; i < indice; i++) {
			String caractere = matcher.group(i + 1);
			int inicio = regex.indexOf(replaceable);
			int fim = inicio + replaceable.length();

			if (fim < regex.length() && "?".equals(String.valueOf(regex.charAt(fim))))
				fim++;

			regex.replace(inicio, fim, String.valueOf(caractere));
		}

		String pontoOpcional = ".?";
		if (regex.toString().contains(pontoOpcional)) {
			int i = regex.indexOf(pontoOpcional);
			char caractere = regex.charAt(i + pontoOpcional.length());

			if (Character.isDigit(caractere))
				regex.deleteCharAt(++i);
		}

		return regex.toString();

		// Cria o objeto Matcher responsável por recuperar os grupos de valores
		// definidos na máscara de formatação da classificação documental.
		// Matcher matcher = Pattern.compile(regex).matcher(nivel);

		// Declara o vetor de níveis da classificação documental.
		// Object[] niveis = null;
		// int nivelFinal = 0;
		//
		// if (matcher.matches()) {
		// Recupera a quantidade de grupos configurada na expressão regular.
		// int grupos = matcher.groupCount();

		// Calcula o último nível da classificação documental a ser considerado na
		// geração da máscara SQL.
		// if (niveisAbaixo) {
		// nivelFinal = grupos;
		// } else {
		// // Itera sobre a quantidade de níveis para incrementar o valor do nível
		// final.
		// for (int i = 1; i < grupos; i++) {
		// String grupo = matcher.group(i);
		//
		// if (grupo != null && !grupo.trim().isEmpty())
		// nivelFinal++;
		// }

		// Verifica a quantidade de níveis que deve ser considerada para gerar a máscara
		// SQL.
		// if (nivelInicial > nivelFinal)
		// nivelFinal = nivelInicial;
		// }

		// Instancia o vetor de níveis da classificação documental a partir do nível
		// final.
		// niveis = new Object[nivelFinal];

		// Itera sobre a quantidade de níveis definidos para a classificação documental.
		// for (int i = 0; i < niveis.length; i++) {
		// FIXME O valor inicial para os níveis (assuntos) da classificação documental
		// deve começar em 0 (zero).
		// Calcula o nível da classificação documental, cujo valor inicial corresponde a
		// 1.
		// int nivel = i + 1;

		// Verifica se o índice difere dos níveis inicial e final, para atribuir os
		// valores encontrados por meio da expressão regular.
		// if (nivel < nivelInicial)
		// niveis[i] = matcher.group(nivel);

		// FIXME Refatorar código devido à implementação de lógica que considera apenas
		// o método de codificação decimal.
		// if (nivel > nivelInicial && !niveisAbaixo)
		// niveis[i] = 0;
		// }
		// }

		// Caractere especial que, quando utilizado em conjunto com a função SQL LIKE,
		// funciona como um espaço reservado para qualquer caractere simples.
		// final String placeholder = "_";
		// Aplica a formatação MVEL.
		// String mascara = aplicarFormatacaoMvel(getMascaraSaida(), niveis,
		// placeholder);

		// if (mascara == null || mascara.isEmpty())
		// throw new IllegalStateException("O sistema apresentou um estado inválido: a
		// máscara gerada para a consulta da classificação documental é nula ou
		// vazia.");
		//
		// return mascara;
	}

	public String getMscFilho(String codificacao, boolean niveisAbaixo) {
		int nivelInicial = calcularNivel(formatar(codificacao));
		nivelInicial++;
		return getMscFilho(codificacao, nivelInicial, niveisAbaixo);
	}

	/**
	 * Retorna o campo correspondente ao nível indicado
	 * 
	 * @param nivel - Nivel desejado. Baseado em 1.
	 * @param texto - texto com a classificacao documental
	 * @return
	 */
	public String getCampoDaMascara(int nivel, String texto) {
		String txt = formatar(texto);
		if (txt == null || nivel <= 0) {
			return null;
		}

		Pattern pe = Pattern.compile(getMascaraEntrada());
		Matcher me = pe.matcher(txt);

		if (me.matches()) {
			if (me.groupCount() < nivel)
				return null;
			StringBuffer sb = new StringBuffer(txt);
			// nivel++;
			int inicio = me.start(nivel);
			int fim = me.end(nivel);

			if (inicio < 0 || fim < 0) {
				return null;
			}

			return sb.substring(inicio, fim);

		}

		return null;
	}

	/**
	 * FIXME Corrigir documentação do método.
	 * 
	 * Calcula qual é o nível inicial em que se deve procurar os filhos. A lógica é
	 * pegar o primeiro grupo com zero (0).
	 * 
	 * @param codigo - o código de classificacao documental.
	 * @return O nível do plano de classificação correspondente ao código informado.
	 * 
	 * @throws IllegalArgumentException - se o código de classificação documental é
	 *                                  inválido.
	 */
	public int calcularNivel(String codigo) throws IllegalArgumentException {
		// Validar o código de classificação informado.
		if (!validar(codigo))
			throw new IllegalArgumentException("O código de classificação informado é inválido.");

		// Recupera a lista de expressões regulares que permitem identificar cada nível
		// do plano de classificação documental.
		List<String> niveis = listarNiveisRegExPlanoClassificacao();

		// Verifica a correspondência da codificação a um dos níveis do plano de
		// classificação documental.
		for (int i = niveis.size(); i > 0; i--) {
			int index = i - 1;
			String regex = niveis.get(index);//.replace("0","(0)").replace("_", "([0-9])?").replace(".","\\.");

			if (Pattern.matches(regex, codigo))
				return index + 1;
		}

		throw new IllegalStateException(
				"O sistema apresentou um estado inválido: a classificação documental não corresponde a quaisquer dos níveis configurados para o método de codificação.");
	}

	/**
	 * Método responsável por validar o código de classificação atribuído para cada
	 * assunto - no @see <a href=
	 * "http://conarq.arquivonacional.gov.br/conarqhml/images/publicacoes_textos/Codigo_de_classificacao.pdf">Código
	 * de Classificação de Documentos de Arquivo para a Administração Pública:
	 * Atividades-Meio</a>, as funções, atividades, espécies e tipos documentais
	 * são, genericamente, denominados <b>assuntos</b>. A validação tem a finalidade
	 * de identificar a adequação do código de classificação informado ao padrão
	 * definido pelo método de codificação adotado.
	 * 
	 * @param codigo - o código de classificação atribuído para o assunto - classes,
	 *               subclasses, grupos e subgrupos.
	 * @return <code>true</code> se código de classificação corresponder ao padrão
	 *         definido pelo método de codificação adotado.
	 * @throws IllegalArgumentException - se o código de classificação corresponder
	 *                                  a um valor nulo ou vazio.
	 */
	public boolean validar(String codigo) throws IllegalArgumentException {
		// Validação do valor do código de classificação documental.
		if (codigo == null || codigo.equals(""))
			throw new IllegalArgumentException("O valor do código de classificação não pode ser nulo ou vazio.");

		// Recupera a expressão regular definida para a máscara de formatação da
		// classificação documental.
		String mascara = MascaraUtil.getInstance().getMascaraEntrada();

		// Validação do valor da máscara de formatação do código de classificação
		// documental.
		if (mascara == null || mascara.isEmpty())
			throw new IllegalStateException(
					"O sistema apresentou um estado inválido. É necessário cadastrar a propriedade referente à máscara de entrada do código de classificação documental.");

		// Verifica a adequação da codificação informada à máscara de formatação da
		// classificação documental.
		return Pattern.matches(mascara, codigo);
	}

	// FIXME Refatorar código devido à implementação de lógica que considera apenas
	// o método de codificação decimal.
	public String[] getPais(String codigo) {
		// Validar o código de classificação informado.
		if (!validar(codigo))
			throw new IllegalArgumentException("O código de classificação informado é inválido.");

		// Recupera a lista de expressões regulares que permitem identificar cada nível
		// do plano de classificação documental.
		//List<String> regexs = listarNiveisRegExPlanoClassificacao();

		//if (regexs == null || regexs.isEmpty())
		//	throw new IllegalStateException(
	    //		"O sistema apresentou um estado inválido: é necessário cadastrar as propriedades referentes às máscaras dos níveis (assuntos) definidos para o método de codificação da classificação documental.");

		int minimo = 3;

		Matcher matcher = Pattern.compile(getMascaraEntrada()).matcher(codigo);
		String niveis[] = null;

		if (matcher.matches()) {
			int nivel = calcularNivel(codigo);

			if (nivel == 1)
				return niveis;

			niveis = new String[--nivel];

			for (int i = nivel; i > 0; i--) {
				// Atribui o tamanho do vetor de grupos de captura da expressão regular.
				int length = (nivel > minimo) ? nivel : minimo;
				// Grupos de captura da expressão regular referentes ao nível pai.
				Object[] nivelPai = new Object[length];
				// Quantidade de caracteres do nível pai.
				int caracteres = 0;

				for (int j = 0; j < nivelPai.length; j++) {
					if (j < nivel) {
						nivelPai[j] = matcher.group(j + 1);
						caracteres++;
					}
				}

				if (caracteres < minimo)
					for (int j = caracteres; j < minimo; j++)
						nivelPai[j] = 0;

				// Verifica se a máscara apresenta valor nulo ou se corresponde a uma expressão
				// MVEL.
				if (getMascaraSaida().startsWith("(")) {
					niveis[i - 1] = aplicarFormatacaoMvel(getMascaraSaida(), nivelPai, null);
				} else {
					// Aplica a formatação de saída.
					Formatter f = new java.util.Formatter();
					niveis[i - 1] = f.format(getMascaraSaida(), nivelPai).toString();
					f.close();
				}

				// Decrementa o valor do nível.
				nivel--;
			}
		}

		return niveis;
	}

	/**
	 * Substitui um valor pela máscara correspondente
	 * 
	 * @param s    - String a ter o valor substituído. A string deve estar no
	 *             formato da máscara definida em getMascaraentrada().<br/>
	 *             Ex:01.02.03.04
	 * @param mask - Máscara que será aplicada ao valor. A string ser compatível com
	 *             o formato da máscara definida em getMascaraentrada().<br/>
	 *             ex: 05.06.__.__
	 * @return O valor da entrada alterado de acordo com a máscara. Retorna null, em
	 *         caso de erros de formatação ou máscaras incompatíveis.
	 */
	public String substituir(String codificacao, String pai) {
		if (!(validar(codificacao) && validar(pai)))
			throw new IllegalArgumentException("O código de classificação informado é inválido.");

		StringBuffer novaCodificacao = new StringBuffer(codificacao);

		for (int i = 0; i < pai.length(); i++)
			novaCodificacao.replace(i, i + 1, String.valueOf(pai.charAt(i)));

		return novaCodificacao.toString();

		// if (valor == null || masklike == null || valor.length() != masklike.length())
		// {
		// return null;
		// }
		// Pattern p = Pattern.compile(getMascaraEntrada());
		// Matcher mValor = p.matcher(valor);
		//
		// String mascaraLike = masklike.replaceAll("_", "0");
		// Matcher mMaskLike = p.matcher(mascaraLike);
		//
		// if (!mValor.matches() || !mMaskLike.matches()) {
		// return null;
		// }
		//
		// StringBuffer result = new StringBuffer();
		// char[] caracteres = masklike.toCharArray();
		// for (int i = 0; i < caracteres.length; i++) {
		// if (caracteres[i] == '_') {
		// result.append(valor.charAt(i));
		// } else {
		// result.append(caracteres[i]);
		// }
		// }
		//
		// return result.toString();
	}

	public boolean isCodificacao(String texto) {
		return Pattern.matches(getMascaraEntrada(), texto);
	}

	/**
	 * FIXME Refatorar a assinatura do método, para melhor descrever a recuperação
	 * do total de níveis do plano de classificação. Verificar a possibilidade de
	 * mover esta responsabilidade para uma classe do domínio.<br>
	 * Retorna a quantidade de níveis da mascara definida. Por exemplo:
	 * "00.00.00.00" retorna 4. "11-2222", retorna 2 níveis; Foram inseridos vários
	 * caractéres "1" para tentar cobrir o caso das máscareas de tamanho variável.
	 * 
	 * @return O total de níveis do plano de classificação documental.
	 */
	public int getTotalDeNiveisDaMascara() {
		// Recupera a lista de expressões regulares que permitem identificar cada nível
		// do plano de classificação documental.
		List<String> niveis = listarNiveisRegExPlanoClassificacao();
		// Retorna o tamanho da lista de níveis do plano de classificação.
		return niveis.size();

	}

	// FIXME Verificar a possibilidade de mover esta responsabilidade para uma
	// classe do domínio.
	public boolean isUltimoNivel(String codigo) {
		// Validar o código de classificação informado.
		if (!validar(codigo))
			throw new IllegalArgumentException("O código de classificação informado é inválido.");

		// Calcula o valor do último nível do plano de classificação documental.
		int ultimoNivel = getTotalDeNiveisDaMascara();
		// Retorna o resultado da igualdade entre o valor do nível do código de
		// classificação informado e o valor do último nível do plano de classificação.
		return calcularNivel(codigo) == ultimoNivel;
	}

	/**
	 * Método responsável por listar os valores das propriedades atinentes aos
	 * níveis do plano de classificação - classes, subclasses, grupos e subgrupos.
	 * Os valores recuperados correspondem a expressões regulares que permitem
	 * identificar o nível de uma determinada classificação documental.
	 * 
	 * @return A lista de expressões regulares que permitem identificar cada nível
	 *         do plano de classificação - classes, subclasses, grupos e subgrupos.
	 * 
	 * @throws IllegalArgumentException - se a lista de valores das propriedades
	 *                                  atinentes aos níveis do plano de
	 *                                  classificação for nula ou vazia.
	 */
	private List<String> listarNiveisRegExPlanoClassificacao() throws IllegalArgumentException {
		// Recuperação das propriedades de sistema referentes aos níveis do plano de
		// classificação.
		List<String> niveis = SigaExProperties.getExClassificacaoNiveisRegEx();

		if (niveis == null || niveis.isEmpty())
			return null;
		return niveis;
	}

}
