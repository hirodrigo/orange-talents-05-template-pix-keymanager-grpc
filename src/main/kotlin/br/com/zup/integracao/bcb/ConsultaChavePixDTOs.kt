package br.com.zup.integracao.bcb

import br.com.zup.pix.Instituicoes
import br.com.zup.pix.consulta.ChavePixInfo
import java.time.LocalDateTime

class PixKeyDetailsResponse(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
) {
    fun paraChavePixInfo(): ChavePixInfo {
        return ChavePixInfo(
            tipoChave = keyType.domainType()!!,
            chave = this.key,
            tipoConta = bankAccount.accountType.domainType(),
            conta = ChavePixInfo.ContaChavePixInfo(
                tipo = bankAccount.accountType.domainType().toString(),
                nomeInstituicao = Instituicoes.nome(bankAccount.participant),
                nomeTitular = owner.name,
                cpfTitular = owner.taxIdNumber,
                agencia = bankAccount.branch,
                numero = bankAccount.accountNumber
            ),
            registradaEm = createdAt
        )
    }
}
