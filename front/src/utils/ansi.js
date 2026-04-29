import { AnsiUp } from 'ansi_up'

const ansiUp = new AnsiUp()
ansiUp.use_classes = false // On utilise les styles inline pour la simplicité, ou true si on veut gérer via CSS

export const formatAnsi = (text) => {
  if (!text) return ''
  // Remplacer les sauts de ligne par des <br> si nécessaire, 
  // mais ici on est souvent dans des conteneurs white-space: pre-wrap
  return ansiUp.ansi_to_html(text)
}

export default ansiUp
