# Tron Wallet

Tron Wallet is a multifunctional android wallet for the TRON network.

It gives you the possibility to interact quickly and easily with your account or to keep your TRX and other account data safe in a cold wallet setup.

This app offers you one of the safest ways to protect your private data.

# Features
  - **Create Wallet**
    - encrypts private information with a password
    - creates a private/public key pair
    - creates a 24 words recovery phrase (human readable private key recovery phrase) (BIP39)
  - **Import Wallet**
    - import with private key or 24 words recovery phrase
    - or import public address only (watch only setup)
  - **Wallet Functionalities**
    - check balance (TRX, tokens)
    - toggle market price view
    - check frozen amount
    - send TRX and tokens
    - receive using QR code
    - freeze TRX to get votes and bandwidth
    - submit votes for representatives
    - offline signing mechanism with QR code scanning
    - participate on token distributions
    - manually set your node connection
  - **Block Explorer**
    - see latest blocks
    - see latest transactions
    - see representative candidates
    - see connected nodes
    - see token distributions
    - see accounts
    - search filter

# Wallet Setups
  - **Watch only setup**
    - import only your public address
    - completly safe because no private information is accessible
    - you have a full overview of your account
    - creates unsigned transactions (used in combination with a cold wallet setup)
  - **Hot Wallet Setup**
    - owns public and private key
    - full overview of account
    - full access (sending, freezing, voting, ...)
  - **Cold Wallet Setup**
    - minimalistic and safest wallet
    - owns public and private key
    - never connects to the internet (to be completly secure you should never connect your device to the internet)
    - signs transactions (from watch only setup)

# Goal

The goal in the future is to connect the users even better and easier with the TRON network and thus form a basis for all in the TRON community to strengthen them and offer extended possibilities.

#

# Modules
The code is built up in 3 different main modules, the Wallet Module, Transaction Module and the Block Explorer Module.

**The Wallet Module** takes care of an account and creates transactions from submodules and passes them to the transactions module. These submodules include sending, freezing, voting, etc.
https://github.com/Dryec/tron-wallet-android/tree/master/app/src/main/java/com/eletac/tronwallet/wallet

**The Transaction Module** receives an unsigned transaction and signs it with a manual signature using a password (Hot Wallet Setup) or starts the QR mechanism for the Cold Wallet Setup to transfer the unsigned transaction to the Cold Wallet, which will sign it and send it back. If the signed transaction exists in the module, it is transmitted to the Tron network.
https://github.com/Dryec/tron-wallet-android/tree/readme_update/app/src/main/java/com/eletac/tronwallet/wallet/confirm_transaction

![alt text](https://raw.githubusercontent.com/Dryec/tron-wallet-android/master/screenshots/transaction_flow.png)

**The Block Explorer Module** is used to display the tron network, the submodules contain the individual parts of the network, such as blocks, transactions, nodes, tokens, etc., which are interesting for display.
https://github.com/Dryec/tron-wallet-android/tree/master/app/src/main/java/com/eletac/tronwallet/block_explorer

# Private Key
All private key operations such as generation and encryption are based on the wallet-cli by TRON Foundation and can be found here.
https://github.com/tronprotocol/wallet-cli
The private key is stored encrypted and safely in a private context.
https://github.com/Dryec/tron-wallet-android/blob/readme_update/app/src/main/java/org/tron/walletserver/WalletClient.java#L172

# Password
The password is currently limited only by length(6) and whitespaces, further rules will follow.