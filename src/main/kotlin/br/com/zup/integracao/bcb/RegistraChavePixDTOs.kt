package br.com.zup.integracao.bcb

import br.com.zup.pix.ChavePix
import br.com.zup.pix.ContaAssociada
import br.com.zup.pix.TipoChave
import br.com.zup.pix.TipoConta
import java.time.LocalDateTime

data class CreatePixKeyRequest(
    val keyType: PixKeyType,
    val key: String?,
    val bankAccount: BankAccount,
    val owner: Owner
) {

    companion object {
        fun of(chavePix: ChavePix): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                keyType = PixKeyType.by(chavePix.tipoChave),
                key = chavePix.chave,
                bankAccount = BankAccount(
                    participant = ContaAssociada.ITAU_UNIBANCO_ISBP,
                    branch = chavePix.conta.agencia,
                    accountNumber = chavePix.conta.numero,
                    accountType = AccountType.by(chavePix.tipoConta),
                ),
                owner = Owner(
                    type = Owner.OwnerType.NATURAL_PERSON,
                    name = chavePix.conta.titular.nomeTitular,
                    taxIdNumber = chavePix.conta.titular.cpf
                )

            )
        }
    }

}

data class CreatePixKeyResponse(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
)

enum class PixKeyType {
    CPF,
    CNPJ,
    PHONE,
    EMAIL,
    RANDOM;

    companion object {
        fun by(tipoChave: TipoChave): PixKeyType {
            return when (tipoChave) {
                TipoChave.CPF -> CPF
                TipoChave.CELULAR -> PHONE
                TipoChave.EMAIL -> EMAIL
                TipoChave.ALEATORIA -> RANDOM
            }
        }
    }

    fun domainType(): TipoChave? {
        return when (this) {
            CPF -> TipoChave.CPF
            PHONE -> TipoChave.CELULAR
            EMAIL -> TipoChave.EMAIL
            RANDOM -> TipoChave.ALEATORIA
            CNPJ -> null
        }
    }
}

data class BankAccount(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
)

enum class AccountType {
    CACC,
    SVGS;

    companion object {
        fun by(tipoConta: TipoConta): AccountType {
            return when (tipoConta) {
                TipoConta.CONTA_POUPANCA -> SVGS
                TipoConta.CONTA_CORRENTE -> CACC
            }
        }
    }

    fun domainType(): TipoConta {
        return when (this) {
            SVGS -> TipoConta.CONTA_POUPANCA
            CACC -> TipoConta.CONTA_CORRENTE
        }
    }
}


data class Owner(
    val type: OwnerType,
    val name: String,
    val taxIdNumber: String
) {
    enum class OwnerType {
        NATURAL_PERSON,
        LEGAL_PERSON;
    }
}


