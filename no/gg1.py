import requests

#apaga conteudp tela clear
import os
os.system('cls' if os.name == 'nt' else 'clear')


# input duvida: string com a pergunta   
# output resposta: string com a resposta
def gemini(duvida):
    # Formatando a requisição
    requisicao = {
        "modelType": "text_only",
        "prompt": f"Responde em portugues brasileito e de  forma que um humano entenda. O que esse arquivo faz e sua relacaco com os demais? \n\n conteudo:{duvida}"
    }

    # Enviando a requisição e capturando a resposta
    resposta = requests.post("http://localhost:3000/chat-with-gemini", json=requisicao)

    # Exibindo a resposta
    if resposta.status_code == 200:
        resposta_json = resposta.json()
        return resposta_json
    else:
        return None


#salva nome somente dos arquivos do diretorio
import os
arquivos = [arquivo for arquivo in os.listdir() if os.path.isfile(arquivo)]


#imprime arquivos do diretorio  con numwro a esquerda
for i, arquivo in enumerate(arquivos):
    print(f"{i} - {arquivo}")       

#perguntar ao usuario qual arquivo deseja abrir
arquivo = int(input("Digite o número do arquivo que deseja abrir: "))   


#imprime nome do arquivo escolhido
print(f"Você escolheu o arquivo: {arquivos[arquivo]}")  


#verifica se exite arqiuvos no diretorio
if len(arquivos) == 0:
    print("Nenhum arquivo encontrado")
    exit()



#gera gemini com contudo em texto do arquivo escolhido  
with open(arquivos[arquivo], "r") as f:
    conteudo = f.read()
    resposta = gemini(conteudo)
    #print 1 letra r por 0.01segundo
    import time

    print("\n\n\n")

    for i in resposta['result']:
        print(i, end='', flush=True)
        time.sleep(0.05)
        
    print("\n\n\n")
        





