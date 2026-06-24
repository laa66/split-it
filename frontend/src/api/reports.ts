import apiClient from './client';

export async function getReport(groupId: string): Promise<Blob> {
  const { data } = await apiClient.get<Blob>(`/groups/${groupId}/report`, {
    responseType: 'blob',
  });
  return data;
}
