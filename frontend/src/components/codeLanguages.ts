export const codeLanguages = [
  { value: 'plaintext', label: '纯文本' }, { value: 'cpp', label: 'C++' },
  { value: 'python', label: 'Python' }, { value: 'java', label: 'Java' },
  { value: 'javascript', label: 'JavaScript' }, { value: 'sql', label: 'SQL' },
] as const
export function codeLanguage(value?: string | null) {
  if (value == null) return { value: 'cpp', label: 'C++17' }
  return codeLanguages.find(language => language.value === value) || codeLanguages[0]
}
